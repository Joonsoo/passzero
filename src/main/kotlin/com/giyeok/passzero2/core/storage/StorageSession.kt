package com.giyeok.passzero2.core.storage

import com.giyeok.passzero2.core.StorageProto
import kotlinx.coroutines.flow.Flow

interface StorageSession {
  suspend fun getConfig(): StorageProto.Config
  suspend fun getDirectoryList(): List<StorageProto.DirectoryInfo>
  suspend fun streamDirectoryList(): Flow<StorageProto.DirectoryInfo>
  suspend fun getDirectoryInfo(directory: String): StorageProto.DirectoryInfo

  /**
   * Fetches the list of all entries in the {@code directory}.
   */
  suspend fun getEntryList(directory: String): List<StorageProto.Entry>

  suspend fun streamEntryList(directory: String): Flow<StorageProto.Entry>
  suspend fun getEntryDetail(directory: String, entryId: String): StorageProto.EntryDetail

  /**
   * Reads the content of entry list cache file. Returns null if not exists.
   */
  suspend fun getEntryListCache(directory: String): StorageProto.EntryListCache?

  /**
   * Deletes the entry list cache file. Does nothing if the cache file does not exists.
   */
  suspend fun deleteEntryListCache(directory: String)

  /**
   * Creates the entry list cache file from the current entry lists. Returns the entry list.
   */
  suspend fun createEntryListCache(directory: String): StorageProto.EntryListCache
}
