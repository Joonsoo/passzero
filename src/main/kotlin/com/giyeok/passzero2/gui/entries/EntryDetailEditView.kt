package com.giyeok.passzero2.gui.entries

import com.giyeok.passzero2.core.StorageProto
import com.giyeok.passzero2.core.storage.StorageSession
import com.giyeok.passzero2.gui.Config
import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.GridBagLayout
import javax.swing.JPanel

class EntryDetailEditView(
  private val config: Config,
  private val session: StorageSession,
  val entry: StorageProto.Entry?,
) : JPanel() {
  val readyState = MutableStateFlow(false)
  private val gridLayout = GridBagLayout()

  init {
    layout = gridLayout
  }

  fun getEntryInfo(): StorageProto.EntryInfo {
    return StorageProto.EntryInfo.newBuilder().build()
  }

  fun getEntryDetails(): StorageProto.EntryDetail {
    return StorageProto.EntryDetail.newBuilder().addItems(
      StorageProto.EntryDetailItem.newBuilder()
        .setType(StorageProto.EntryDetailItemType.ENTRY_DETAIL_ITEM_USERNAME)
        .setValue("Hahaha")
    ).build()
  }
}
