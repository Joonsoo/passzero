package com.giyeok.passzero2.core.storage

import com.giyeok.passzero2.core.UnsuccessfulResponseException
import com.giyeok.passzero2.core.await
import com.giyeok.passzero2.core.storage.DropboxClient.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.google.protobuf.ByteString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

interface DropboxClient {
  data class DropboxError(val detail: DropboxErrorDetail) : Exception()
  data class DropboxErrorDetail(val errorSummary: String)

  data class ListFileEntry(
    @SerializedName(".tag") val tag: String,
    val name: String,
    val id: String,
    val clientModified: String,
    val serverModified: String
  )

  suspend fun writeRaw(path: String, bytes: ByteString)
  suspend fun readRaw(path: String): ByteString

  suspend fun getFileList(path: String): List<ListFileEntry>
  fun streamFileList(path: String): Flow<ListFileEntry>

  suspend fun deleteFile(path: String)
  suspend fun deleteDirectory(path: String)
  suspend fun createFolder(path: String)
}

data class DropboxToken(val accessToken: String, val refreshToken: String)

class DropboxClientImpl(
  val appKey: String,
  initialToken: DropboxToken,
  private val okHttpClient: OkHttpClient,
  private val gson: Gson,
  private val accessTokenUpdateListener: (suspend (DropboxToken) -> Unit)?
) : DropboxClient {
  private val applicationJson = "application/json".toMediaType()

  private val requestMutex = Mutex()
  private val tokenFlow = MutableStateFlow(initialToken)

  private fun UnsuccessfulResponseException.toDropboxError(): Exception =
    if (this.responseBody == null) this else try {
      DropboxError(gson.fromJson(this.responseBody, DropboxErrorDetail::class.java))
    } catch (e: JsonSyntaxException) {
      this
    }

  private suspend inline fun sendRequestRaw(request: Request): Response =
    try {
      okHttpClient.newCall(request).await()
    } catch (e: UnsuccessfulResponseException) {
      if (e.responseBody != null) {
        throw e.toDropboxError()
      }
      throw e
    }

  data class RefreshTokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Int
  )

  suspend fun refreshToken(): DropboxToken {
    val refreshToken = tokenFlow.value.refreshToken
    val response = sendRequestRaw(
      Request.Builder()
        .url("https://api.dropboxapi.com/oauth2/token")
        .post(
          FormBody.Builder()
            .addEncoded("grant_type", "refresh_token")
            .addEncoded("client_id", appKey)
            .addEncoded("refresh_token", tokenFlow.value.refreshToken)
            .build()
        ).build()
    )
    val responseBody = response.body!!.jsonTo<RefreshTokenResponse>()

    return DropboxToken(responseBody.accessToken, refreshToken)
  }

  private suspend inline fun sendRequest(crossinline requestBuilder: () -> Request): Response =
    requestMutex.withLock {
      try {
        sendRequestRaw(requestBuilder())
      } catch (e: DropboxError) {
        if (e.detail.errorSummary.startsWith("expired_access_token/")) {
          val newToken = refreshToken()
          accessTokenUpdateListener?.invoke(newToken)
          tokenFlow.tryEmit(newToken)
          sendRequestRaw(requestBuilder())
        } else {
          throw e
        }
      }
    }

  private fun Request.Builder.addApiKeyHeader(): Request.Builder =
    this.header("Authorization", "Bearer ${tokenFlow.value.accessToken}")

  data class ListFolderContinueReq(val cursor: String)

  private inline fun <reified T> ResponseBody.jsonTo() = this.charStream().use { stream ->
    gson.fromJson(stream, T::class.java)!!
  }

  data class ListFolderReq(val path: String, val recursive: Boolean)
  data class ListFolderRes(
    val entries: List<ListFileEntry>,
    val cursor: String,
    val hasMore: Boolean
  )

  private suspend fun listFolder(req: ListFolderReq): ListFolderRes {
    return sendRequest {
      Request.Builder()
        .url("https://api.dropboxapi.com/2/files/list_folder")
        .addApiKeyHeader()
        .addHeader("Content-Type", "application/json")
        .post(gson.toJson(req).toRequestBody(applicationJson))
        .build()
    }.body!!.jsonTo<ListFolderRes>()
  }

  private suspend fun listFolderContinue(cursor: String): ListFolderRes {
    return sendRequest {
      Request.Builder()
        .url("https://api.dropboxapi.com/2/files/list_folder/continue")
        .addApiKeyHeader()
        .addHeader("Content-Type", "application/json")
        .post(
          gson.toJson(ListFolderContinueReq(cursor))
            .toRequestBody(applicationJson)
        ).build()
    }.body!!.jsonTo<ListFolderRes>()
  }

  data class UploadReq(val path: String, val mode: String)

  override suspend fun writeRaw(path: String, bytes: ByteString) {
    val response = sendRequest {
      Request.Builder()
        .url("https://content.dropboxapi.com/2/files/upload")
        .addApiKeyHeader()
        .addHeader("Dropbox-API-Arg", gson.toJson(UploadReq(path, "overwrite")))
        .addHeader("Content-Type", "application/octet-stream")
        .post(bytes.toByteArray().toRequestBody()).build()
    }
    response.close()
  }


  override suspend fun getFileList(path: String): List<ListFileEntry> {
    var lastResponse = listFolder(ListFolderReq(path, false))
    val entries = mutableListOf<ListFileEntry>()
    entries.addAll(lastResponse.entries)
    while (lastResponse.hasMore) {
      lastResponse = listFolderContinue(lastResponse.cursor)
      entries.addAll(lastResponse.entries)
    }
    return entries
  }

  override fun streamFileList(path: String): Flow<ListFileEntry> = flow {
    var lastResponse = listFolder(ListFolderReq(path, false))
    lastResponse.entries.forEach { emit(it) }
    while (lastResponse.hasMore) {
      lastResponse = listFolderContinue(lastResponse.cursor)
      lastResponse.entries.forEach { emit(it) }
    }
  }

  data class DownloadReq(val path: String)

  override suspend fun readRaw(path: String): ByteString {
    val response = sendRequest {
      Request.Builder()
        .url("https://content.dropboxapi.com/2/files/download")
        .addApiKeyHeader()
        .addHeader("Dropbox-API-Arg", gson.toJson(DownloadReq(path))).build()
    }
    return ByteString.readFrom(response.body!!.byteStream())
  }

  data class DeleteFileReq(val path: String)

  override suspend fun deleteFile(path: String) {
    val response = sendRequest {
      Request.Builder()
        .url("https://api.dropboxapi.com/2/files/delete_v2")
        .addApiKeyHeader()
        .addHeader("Content-Type", "application/json")
        .post(gson.toJson(DeleteFileReq(path)).toRequestBody(applicationJson))
        .build()
    }
    response.close()
  }

  override suspend fun deleteDirectory(path: String) {
    deleteFile(path)
  }

  data class CreateFolderReq(val path: String, val autorename: Boolean)

  override suspend fun createFolder(path: String) {
    val response = sendRequest {
      Request.Builder()
        .url("https://api.dropboxapi.com/2/files/create_folder_v2")
        .addApiKeyHeader()
        .addHeader("Content-Type", "application/json")
        .post(gson.toJson(CreateFolderReq(path, false)).toRequestBody(applicationJson))
        .build()
    }
    response.close()
  }
}
