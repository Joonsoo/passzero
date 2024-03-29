package com.giyeok.passzero2.core.storage

import com.giyeok.passzero2.core.CryptSession
import com.giyeok.passzero2.core.LocalInfoProto
import com.giyeok.passzero2.core.StorageProto
import com.giyeok.passzero2.core.StorageProto.DirectoryInfo
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.protobuf.ByteString
import com.google.protobuf.util.JsonFormat
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import kotlin.random.Random

class DropboxSession(
  private val cryptSession: CryptSession,
  private val appRootPath: String,
  val client: DropboxClient,
  val gson: Gson,
) : StorageSession {
  constructor(
    cryptSession: CryptSession,
    profile: LocalInfoProto.DropboxStorageProfile,
    okHttpClient: OkHttpClient,
    gson: Gson = defaultGson(),
    accessTokenUpdateListener: (suspend (DropboxToken) -> Unit)?
  ) : this(
    cryptSession,
    profile.appRootPath,
    DropboxClientImpl(
      profile.appKey,
      DropboxToken(profile.accessToken, profile.refreshToken),
      okHttpClient,
      gson,
      accessTokenUpdateListener
    ),
    gson
  )

  private val cacheMutex = Mutex()
  private val cacheFlow = MutableStateFlow<StorageProto.EntryListCache?>(null)

  companion object {
    fun defaultGson(): Gson {
      return GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()
    }
  }

  private suspend fun writeEncoded(path: String, bytes: ByteString) {
    client.writeRaw(path, cryptSession.encode(bytes))
  }

  private suspend fun readDecoded(path: String): ByteString {
    val configRaw = client.readRaw(path)
    return cryptSession.decode(configRaw)
  }

  private fun revisionRoot(): String = "$appRootPath/${cryptSession.revision}"
  private fun directoryRoot(directory: String): String = "${revisionRoot()}/$directory"
  private fun entryRoot(directory: String, entryId: String): String =
    "${directoryRoot(directory)}/$entryId"

  private fun configPath(): String = "${revisionRoot()}/config"

  private fun directoryPath(directory: String): String =
    "$appRootPath/${cryptSession.revision}/$directory"

  override suspend fun getConfig(): StorageProto.Config {
    val json = readDecoded(configPath()).toStringUtf8()
    val builder = StorageProto.Config.newBuilder()
    JsonFormat.parser().merge(json, builder)
    return builder.build()
  }

  override suspend fun writeConfig(config: StorageProto.Config) {
    writeEncoded(configPath(), ByteString.copyFromUtf8(JsonFormat.printer().print(config)))
  }

  override suspend fun createDirectory(directoryName: String): DirectoryInfo {
    val directoryId =
      "${System.currentTimeMillis()}_${String.format("%04d", Random.nextInt(10000))}"
    client.createFolder(directoryPath(directoryId))
    val directoryInfo = DirectoryInfo.newBuilder()
      .setId(directoryId)
      .setName(directoryName)
      .build()
    writeDirectoryInfo(directoryInfo)
    return directoryInfo
  }

  override suspend fun writeDirectoryInfo(info: DirectoryInfo) {
    writeEncoded(
      "${directoryPath(info.id)}/info",
      ByteString.copyFromUtf8(JsonFormat.printer().print(info))
    )
  }

  override suspend fun getDirectoryList(): List<StorageProto.DirectoryInfo> = coroutineScope {
    client.getFileList(revisionRoot()).filter { it.tag == "folder" }.map {
      async { getDirectoryInfo(it.name) }
    }.map { it.await() }
  }

  @FlowPreview
  override fun streamDirectoryList(): Flow<StorageProto.DirectoryInfo> {
    val directories = client.streamFileList(revisionRoot()).filter { it.tag == "folder" }
    return directories.flatMapMerge { file ->
      flow { emit(getDirectoryInfo(file.name)) }
    }
  }

  override suspend fun getDirectoryInfo(directory: String): StorageProto.DirectoryInfo {
    val json = readDecoded("${directoryPath(directory)}/info").toStringUtf8()
    val builder = StorageProto.DirectoryInfo.newBuilder()
    JsonFormat.parser().merge(json, builder)
    builder.id = directory
    return builder.build()
  }

  data class EntryInfo(val name: String, val type: String)

  private suspend fun getEntryInfo(directory: String, entryId: String): StorageProto.Entry {
    val entryDirectory = entryRoot(directory, entryId)
    try {
      val proto = readDecoded("$entryDirectory/info2")
      return StorageProto.Entry.newBuilder().setDirectory(directory).setId(entryId)
        .setInfo(StorageProto.EntryInfo.parseFrom(proto)).build()
    } catch (e: DropboxClient.DropboxError) {
      if (e.detail.errorSummary.startsWith("path/not_found/")) {
        val entryInfoBuilder = StorageProto.EntryInfo.newBuilder()
        try {
          val infoJson = readDecoded("$entryDirectory/info").toStringUtf8()
          val entryInfo = gson.fromJson(infoJson, EntryInfo::class.java)
          entryInfoBuilder.name = entryInfo.name
          entryInfoBuilder.type =
            StorageProto.EntryType.valueOf("ENTRY_TYPE_${entryInfo.type.uppercase()}")
        } catch (e: Exception) {
          println("Failed to load info of $entryId")
          // Ignore
        }
        return StorageProto.Entry.newBuilder().setDirectory(directory).setId(entryId)
          .setInfo(entryInfoBuilder).build()
      }
      throw e
    }
  }

  override suspend fun getEntryList(directory: String): List<StorageProto.Entry> = coroutineScope {
    val directoryRoot = directoryRoot(directory)
    val entryFolders = client.getFileList(directoryRoot).filter { it.tag == "folder" }

    val entries = entryFolders.map { file ->
      async { getEntryInfo(directory, file.name) }
    }
    entries.awaitAll()
  }

  @FlowPreview
  override fun streamEntryList(directory: String): Flow<StorageProto.Entry> {
    val directoryRoot = directoryRoot(directory)
    val entryFolders = client.streamFileList(directoryRoot).filter { it.tag == "folder" }

    return entryFolders.flatMapMerge { file ->
      flowOf(getEntryInfo(directory, file.name))
    }
  }

  data class EntryDetailItem(val key: String, val v: String) {
    fun toProto(): StorageProto.EntryDetailItem =
      StorageProto.EntryDetailItem.newBuilder()
        .setType(StorageProto.EntryDetailItemType.valueOf("ENTRY_DETAIL_ITEM_${key.uppercase()}"))
        .setValue(v).build()
  }

  override suspend fun getEntryDetail(
    directory: String,
    entryId: String
  ): StorageProto.EntryDetail {
    val entryDirectory = entryRoot(directory, entryId)
    val proto = try {
      readDecoded("$entryDirectory/detail2")
    } catch (e: DropboxClient.DropboxError) {
      if (e.detail.errorSummary.startsWith("path/not_found/")) {
        val json = readDecoded("$entryDirectory/detail").toStringUtf8()
        val detailItems = gson.fromJson<List<EntryDetailItem>>(
          json,
          object : TypeToken<List<EntryDetailItem>>() {}.type
        )
        return StorageProto.EntryDetail.newBuilder().addAllItems(detailItems.map { it.toProto() })
          .build()
      }
      throw e
    }
    return StorageProto.EntryDetail.parseFrom(proto)
  }

  override suspend fun createEntry(
    directory: String,
    entryInfo: StorageProto.EntryInfo,
    detail: StorageProto.EntryDetail
  ): StorageProto.Entry {
    val entryId = "${System.currentTimeMillis()}_${String.format("%04d", Random.nextInt(10000))}"
    val entryRoot = entryRoot(directory, entryId)
    client.createFolder(entryRoot)
    writeEncoded("$entryRoot/info2", entryInfo.toByteString())
    writeEncoded("$entryRoot/detail2", detail.toByteString())
    val newEntry = StorageProto.Entry.newBuilder()
      .setDirectory(directory)
      .setId(entryId)
      .setInfo(entryInfo)
      .build()
    updateCache(directory) { cache ->
      cache.toBuilder().addEntries(newEntry).build()
    }
    return newEntry
  }

  override suspend fun updateEntry(
    directory: String,
    entryId: String,
    entryInfo: StorageProto.EntryInfo,
    detail: StorageProto.EntryDetail
  ) {
    val root = entryRoot(directory, entryId)
    writeEncoded("$root/info2", entryInfo.toByteString())
    writeEncoded("$root/detail2", detail.toByteString())
    updateCache(directory) { cache ->
      val updatingIdx =
        cache.entriesList.indexOfFirst { it.directory == directory && it.id == entryId }
      if (updatingIdx < 0) {
        null
      } else {
        val updatingEntry = cache.getEntries(updatingIdx)
        check(updatingEntry.directory == directory && updatingEntry.id == entryId)
        if (updatingEntry.info == entryInfo) {
          // updating entry의 정보가 바뀌지 않았으면 스킵
          null
        } else {
          cache.toBuilder().setEntries(
            updatingIdx,
            StorageProto.Entry.newBuilder()
              .setDirectory(directory)
              .setId(entryId)
              .setInfo(entryInfo)
          ).build()
        }
      }
    }
  }

  override suspend fun deleteEntry(directory: String, entryId: String) {
    val root = entryRoot(directory, entryId)
    try {
      client.deleteFile("$root/detail")
    } catch (_: DropboxClient.DropboxError) {
    }
    try {
      client.deleteFile("$root/info")
    } catch (_: DropboxClient.DropboxError) {
    }
    try {
      client.deleteFile("$root/detail2")
    } catch (_: DropboxClient.DropboxError) {
    }
    try {
      client.deleteFile("$root/info2")
    } catch (_: DropboxClient.DropboxError) {
    }
    try {
      client.deleteDirectory(root)
    } catch (_: DropboxClient.DropboxError) {
    }
    updateCache(directory) { cache ->
      val deletingIdx =
        cache.entriesList.indexOfFirst { it.directory == directory && it.id == entryId }
      if (deletingIdx < 0) {
        null
      } else {
        cache.toBuilder().removeEntries(deletingIdx).build()
      }
    }
  }

  private fun entryCachePath(directory: String): String =
    "$appRootPath/${cryptSession.revision}/$directory/info_cache"

  private suspend fun readCache(directory: String): StorageProto.EntryListCache? {
    val bytes = try {
      readDecoded(entryCachePath(directory))
    } catch (_: DropboxClient.DropboxError) {
      return null
    }
    val cache = try {
      StorageProto.EntryListCache.parseFrom(bytes)
    } catch (_: Exception) {
      return null
    }
    cacheFlow.emit(cache)
    return cache
  }

  private suspend fun writeCache(directory: String, cache: StorageProto.EntryListCache) {
    writeEncoded(entryCachePath(directory), cache.toByteString())
    cacheFlow.emit(cache)
  }

  private suspend fun updateCache(
    directory: String,
    updater: (StorageProto.EntryListCache) -> StorageProto.EntryListCache?
  ) {
    val cache = cacheFlow.value
    if (cache != null) {
      cacheMutex.withLock {
        val newCache = updater(cache)
        if (newCache != null) {
          writeCache(directory, newCache)
        }
      }
    }
  }

  override suspend fun getEntryListCache(directory: String): StorageProto.EntryListCache? {
    return try {
      readCache(directory)
    } catch (e: DropboxClient.DropboxError) {
      if (e.detail.errorSummary.startsWith("path/not_found/")) {
        return null
      }
      throw e
    }
  }

  override suspend fun deleteEntryListCache(directory: String) {
    client.deleteFile(entryCachePath(directory))
    cacheFlow.emit(null)
  }

  override suspend fun createEntryListCache(directory: String): StorageProto.EntryListCache {
    val cache =
      StorageProto.EntryListCache.newBuilder().addAllEntries(getEntryList(directory)).build()

    writeCache(directory, cache)
    return cache
  }

  @FlowPreview
  override suspend fun createEntryListCacheStreaming(directory: String): Flow<StorageProto.Entry> {
    val stream = streamEntryList(directory)
    val list = mutableListOf<StorageProto.Entry>()
    val mutex = Mutex()

    return stream.onEach { entry ->
      mutex.withLock {
        list.add(entry)
      }
    }.onCompletion {
      val cache = StorageProto.EntryListCache.newBuilder().addAllEntries(list).build()
      writeCache(directory, cache)
    }
  }

  // TODO config 변경 기능 구현
  // TODO directory 생성/편집/삭제 기능 구현
  // createDirectory()
  // updateDirectoryInfo()
  // deleteDirectory()

  // TODO entry 생성, 편집, 삭제 기능은 테스트 안해봄
}
