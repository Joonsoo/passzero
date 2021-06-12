package com.giyeok.passzero2.core.storage

import com.giyeok.passzero2.core.CryptSession
import com.giyeok.passzero2.core.LocalInfoProto
import com.giyeok.passzero2.core.StorageProto
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.protobuf.ByteString
import com.google.protobuf.TypeRegistry
import com.google.protobuf.util.JsonFormat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient

class DropboxSession(
  private val cryptSession: CryptSession,
  private val appRootPath: String,
  val client: DropboxClient,
) : StorageSession {
  constructor(
    cryptSession: CryptSession,
    profile: LocalInfoProto.DropboxStorageProfile,
    okHttpClient: OkHttpClient,
    gson: Gson = defaultGson()
  ) : this(cryptSession, profile.appRootPath, DropboxClient(profile.accessToken, okHttpClient, gson))

  companion object {
    fun defaultGson(): Gson {
      return GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
    }
  }

  private suspend fun writeEncoded(path: String, bytes: ByteString) {
    client.writeRaw(path, cryptSession.encode(bytes))
  }

  private suspend fun readDecoded(path: String): ByteString {
    val configRaw = client.readRaw(path)
    return cryptSession.decode(configRaw)
  }

  override suspend fun getConfig(): StorageProto.Config {
    val json = readDecoded("$appRootPath/${cryptSession.revision}/config").toStringUtf8()
    val builder = StorageProto.Config.newBuilder()
    JsonFormat.parser().merge(json, builder)
    return builder.build()
  }

  override suspend fun getDirectoryList(): List<StorageProto.DirectoryInfo> {
    TODO()
  }

  override suspend fun streamDirectoryList(): Flow<StorageProto.DirectoryInfo> {
    TODO()
  }

  override suspend fun getDirectoryInfo(directory: String): StorageProto.DirectoryInfo {
    val json = readDecoded("$appRootPath/${cryptSession.revision}/$directory/info").toStringUtf8()
    val builder = StorageProto.DirectoryInfo.newBuilder()
    JsonFormat.parser().merge(json, builder)
    return builder.build()
  }

  private fun directoryRoot(directory: String): String = "$appRootPath/${cryptSession.revision}/$directory"

  data class EntryInfo(val name: String, val type: String)

  private suspend fun getEntryInfo(directory: String, id: String, infoFilePath: String): StorageProto.Entry {
    val entryInfoBuilder = StorageProto.EntryInfo.newBuilder()
    try {
      val infoJson = readDecoded(infoFilePath).toStringUtf8()
      val entryInfo = client.gson.fromJson(infoJson, EntryInfo::class.java)
      entryInfoBuilder.name = entryInfo.name
      entryInfoBuilder.type = StorageProto.EntryType.valueOf("ENTRY_TYPE_${entryInfo.type.uppercase()}")
    } catch (e: Exception) {
      println("Failed to load info of $id")
      // Ignore
    }
    return StorageProto.Entry.newBuilder().setDirectory(directory).setId(id).setInfo(entryInfoBuilder).build()
  }

  override suspend fun getEntryList(directory: String): List<StorageProto.Entry> = coroutineScope {
    val directoryRoot = directoryRoot(directory)
    val entryFolders = client.getFileList(directoryRoot).filter { it.tag == "folder" }

    val entries = entryFolders.map { file ->
      async { getEntryInfo(directory, file.name, "$directoryRoot/${file.name}/info") }
    }
    entries.awaitAll()
  }

  @ExperimentalCoroutinesApi
  @FlowPreview
  override suspend fun streamEntryList(directory: String): Flow<StorageProto.Entry> {
    val directoryRoot = directoryRoot(directory)
    val entryFolders = client.streamFileList(directoryRoot).filter { it.tag == "folder" }

    return entryFolders.flatMapMerge { file ->
      flow { emit(getEntryInfo(directory, file.name, "$directoryRoot/${file.name}/info")) }
    }
  }

  private fun entryPath(directory: String, entryId: String): String =
    "$appRootPath/${cryptSession.revision}/$directory/$entryId"

  data class EntryDetailItem(val key: String, val v: String) {
    fun toProto(): StorageProto.EntryDetailItem =
      StorageProto.EntryDetailItem.newBuilder()
        .setType(StorageProto.EntryDetailItemType.valueOf("ENTRY_DETAIL_ITEM_${key.uppercase()}"))
        .setValue(v).build()
  }

  override suspend fun getEntryDetail(directory: String, entryId: String): StorageProto.EntryDetail {
    // detail2 파일이 있으면 그 파일을 사용하고 없으면 legacy로 읽어오고
    val json = readDecoded("${entryPath(directory, entryId)}/detail").toStringUtf8()
    val detailItems =
      client.gson.fromJson<List<EntryDetailItem>>(json, object : TypeToken<List<EntryDetailItem>>() {}.type)
    return StorageProto.EntryDetail.newBuilder().addAllItems(detailItems.map { it.toProto() }).build()
  }

  private fun entryCachePath(directory: String): String =
    "$appRootPath/${cryptSession.revision}/$directory/info_cache"

  override suspend fun getEntryListCache(directory: String): StorageProto.EntryListCache? {
    return try {
      StorageProto.EntryListCache.parseFrom(readDecoded(entryCachePath(directory)))
    } catch (e: DropboxClient.DropboxError) {
      if (e.detail.errorSummary.startsWith("path/not_found/")) {
        return null
      }
      throw e
    }
  }

  override suspend fun deleteEntryListCache(directory: String) {
    client.deleteFile(entryCachePath(directory))
  }

  override suspend fun createEntryListCache(directory: String): StorageProto.EntryListCache {
    val entryList = StorageProto.EntryListCache.newBuilder().addAllEntries(getEntryList(directory)).build()

    writeEncoded(entryCachePath(directory), entryList.toByteString())
    return entryList
  }

  // TODO: directory, entry 생성/편집/삭제 기능 구현
  // createDirectory()
  // updateDirectoryInfo()
  // deleteDirectory()

  // createEntry()
  // updateEntry()
  // deleteEntry()
}
