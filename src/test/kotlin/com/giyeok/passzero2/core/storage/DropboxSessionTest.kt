package com.giyeok.passzero2.core.storage

import com.giyeok.passzero2.core.CryptSession
import com.giyeok.passzero2.core.LocalInfoWithRevision
import com.giyeok.passzero2.core.StorageProto
import com.google.protobuf.ByteString
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import java.io.File

class DropboxSessionTest {
  @Test
  fun test(): Unit = runBlocking {
    // TODO 돌리기 전에 pw와 local info 경로 설정
    val pw = "asdf"
    val localInfo = LocalInfoWithRevision.decode(
      pw, ByteString.readFrom(File("./localInfo.p0").inputStream())
    )
    val cryptSession = CryptSession.from(localInfo.localInfoWithRevision, pw)
    val okHttpClient = OkHttpClient()
    val dropboxSession =
      DropboxSession(cryptSession, localInfo.localInfoWithRevision.localInfo.storageProfile.dropbox, okHttpClient)

    val config = dropboxSession.getConfig()
    val newEntry = dropboxSession.createEntry(
      config.defaultDirectory,
      StorageProto.EntryInfo.newBuilder()
        .setType(StorageProto.EntryType.ENTRY_TYPE_LOGIN)
        .setName("testing_entry").build(),
      StorageProto.EntryDetail.newBuilder().addItems(
        StorageProto.EntryDetailItem.newBuilder()
          .setType(StorageProto.EntryDetailItemType.ENTRY_DETAIL_ITEM_USERNAME)
          .setValue("user_id")
      ).build()
    )
    println(newEntry)

    val newCache = dropboxSession.createEntryListCache(config.defaultDirectory)
    println(newCache.entriesList.find { it.id == newEntry.id })

    val fetched = dropboxSession.getEntryDetail(newEntry.directory, newEntry.id)
    println(fetched)
  }
}
