package com.giyeok.passzero2.core.storage

import com.giyeok.passzero2.core.UnsuccessfulResponseException
import com.giyeok.passzero2.core.await
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.google.protobuf.ByteString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody

class DropboxClient(
  private val accessToken: String,
  private val okHttpClient: OkHttpClient,
  val gson: Gson = DropboxSession.defaultGson()
) {
  private val applicationJson = "application/json".toMediaType()

  data class DropboxError(val detail: DropboxErrorDetail) : Exception()
  data class DropboxErrorDetail(val errorSummary: String)

  private fun UnsuccessfulResponseException.toDropboxError(): Exception =
    if (this.responseBody == null) this else try {
      DropboxError(gson.fromJson(this.responseBody, DropboxErrorDetail::class.java))
    } catch (e: JsonSyntaxException) {
      this
    }

  private suspend fun <T> sendRequest(request: Request, responseHandler: (Response) -> T): T = try {
    okHttpClient.newCall(request).await().use { responseHandler(it) }
  } catch (e: UnsuccessfulResponseException) {
    if (e.responseBody != null) {
      throw e.toDropboxError()
    }
    throw e
  }

  fun Request.Builder.addApiKeyHeader(): Request.Builder =
    this.header("Authorization", "Bearer $accessToken")

  data class ListFolderReq(val path: String, val recursive: Boolean)
  data class ListFolderRes(
    val entries: List<ListFileEntry>,
    val cursor: String,
    val hasMore: Boolean
  )

  data class ListFileEntry(
    @SerializedName(".tag") val tag: String,
    val name: String,
    val id: String,
    val clientModified: String,
    val serverModified: String
  )

  data class ListFolderContinueReq(val cursor: String)

  inline fun <reified T> ResponseBody.toProtoMessage() = this.charStream().use { stream ->
    gson.fromJson(stream, T::class.java)!!
  }

  suspend fun listFolder(req: ListFolderReq): ListFolderRes {
    return sendRequest(
      Request.Builder()
        .url("https://api.dropboxapi.com/2/files/list_folder")
        .addApiKeyHeader()
        .addHeader("Content-Type", "application/json")
        .post(gson.toJson(req).toRequestBody("application/json".toMediaType())).build()
    ) { response -> response.body!!.toProtoMessage<ListFolderRes>() }
  }

  suspend fun listFolderContinue(cursor: String): ListFolderRes {
    return sendRequest(
      Request.Builder()
        .url("https://api.dropboxapi.com/2/files/list_folder/continue")
        .addApiKeyHeader()
        .addHeader("Content-Type", "application/json")
        .post(
          gson.toJson(ListFolderContinueReq(cursor)).toRequestBody("application/json".toMediaType())
        )
        .build()
    ) { response -> response.body!!.toProtoMessage<ListFolderRes>() }
  }

  data class UploadReq(val path: String, val mode: String)

  suspend fun writeRaw(path: String, bytes: ByteString) {
    sendRequest(
      Request.Builder()
        .url("https://content.dropboxapi.com/2/files/upload")
        .addApiKeyHeader()
        .addHeader("Dropbox-API-Arg", gson.toJson(UploadReq(path, "overwrite")))
        .addHeader("Content-Type", "application/octet-stream")
        .post(bytes.toByteArray().toRequestBody()).build()
    ) { it.close() }
  }


  suspend fun getFileList(path: String): List<ListFileEntry> {
    var lastResponse = listFolder(ListFolderReq(path, false))
    val entries = mutableListOf<ListFileEntry>()
    entries.addAll(lastResponse.entries)
    while (lastResponse.hasMore) {
      lastResponse = listFolderContinue(lastResponse.cursor)
      entries.addAll(lastResponse.entries)
    }
    return entries
  }

  fun streamFileList(path: String): Flow<ListFileEntry> = flow {
    var lastResponse = listFolder(ListFolderReq(path, false))
    lastResponse.entries.forEach { emit(it) }
    while (lastResponse.hasMore) {
      lastResponse = listFolderContinue(lastResponse.cursor)
      lastResponse.entries.forEach { emit(it) }
    }
  }

  data class DownloadReq(val path: String)

  suspend fun readRaw(path: String): ByteString {
    return sendRequest(
      Request.Builder()
        .url("https://content.dropboxapi.com/2/files/download")
        .addApiKeyHeader()
        .addHeader("Dropbox-API-Arg", gson.toJson(DownloadReq(path))).build()
    ) { response -> ByteString.readFrom(response.body!!.byteStream()) }
  }

  data class DeleteFileReq(val path: String)

  suspend fun deleteFile(path: String) {
    sendRequest(
      Request.Builder()
        .url("https://api.dropboxapi.com/2/files/delete_v2")
        .addApiKeyHeader()
        .addHeader("Content-Type", "application/json")
        .post(gson.toJson(DeleteFileReq(path)).toRequestBody(applicationJson))
        .build()
    ) { it.close() }
  }

  data class CreateFolderReq(val path: String, val autorename: Boolean)

  suspend fun createFolder(path: String) {
    sendRequest(
      Request.Builder()
        .url("https://api.dropboxapi.com/2/files/create_folder_v2")
        .addApiKeyHeader()
        .addHeader("Content-Type", "application/json")
        .post(gson.toJson(CreateFolderReq(path, false)).toRequestBody(applicationJson))
        .build()
    ) { it.close() }
  }
}
