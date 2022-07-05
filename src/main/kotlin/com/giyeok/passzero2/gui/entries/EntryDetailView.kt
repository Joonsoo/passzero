package com.giyeok.passzero2.gui.entries

import com.giyeok.passzero2.core.StorageProto
import com.giyeok.passzero2.core.StorageProto.EntryDetail
import com.giyeok.passzero2.core.storage.StorageSession
import com.giyeok.passzero2.gui.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class EntryDetailView(
  private val config: Config,
  private val session: StorageSession,
  val entry: StorageProto.Entry
) : JPanel() {
  private val gridLayout = GridBagLayout()
  private var loadingIcon: JComponent? = null
  var entryDetailRequestJob: Job? = null

  init {
    layout = gridLayout

    SwingUtilities.invokeLater {
      removeAll()
      addKeyValue(0, config.getString("ENTRY_NAME"), entry.info.name)
      addKeyValue(1, config.getString("ENTRY_TYPE"), config.getString(entry.info.type.toString()))

      val gbc = GridBagConstraints()
      gbc.gridx = 0
      gbc.gridy = 2
      gbc.fill = GridBagConstraints.HORIZONTAL
      gbc.insets = Insets(4, 10, 4, 3)
      loadingIcon = JLabel(config.getString("LOADING"))
      add(loadingIcon!!, gbc)
    }

    entryDetailRequestJob = CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
      try {
        val entryDetail = session.getEntryDetail(entry.directory, entry.id)
        showDetails(entryDetail)
      } catch (e: Exception) {
        showError(e)
        e.printStackTrace()
      }
    }
  }

  fun cancelRequest() {
    // 기존에 요청한 내용이 있으면 취소
    entryDetailRequestJob?.cancel()
  }

  private fun showDetails(entryDetail: EntryDetail) {
    SwingUtilities.invokeLater {
      loadingIcon?.let { remove(it) }
      entryDetail.itemsList?.forEachIndexed { index, item ->
        when (item.type) {
          StorageProto.EntryDetailItemType.ENTRY_DETAIL_ITEM_PASSWORD ->
            addPassword(index + 2, item.value)
          else ->
            addKeyValue(index + 2, config.getString(item.type.toString()), item.value)
        }
      }
      parent?.revalidate()
      parent?.repaint()
    }
  }

  private fun showError(error: Exception) {
    SwingUtilities.invokeLater {
      loadingIcon?.let { remove(it) }
      val errorText = JTextArea(error.toString())
      errorText.font = config.defaultFont
      errorText.isEditable = false
      val directoryText = JTextArea(entry.directory)
      directoryText.font = config.defaultFont
      directoryText.isEditable = false
      val idText = JTextArea(entry.id)
      idText.font = config.defaultFont
      idText.isEditable = false
      addRow(2, JLabel(config.getString("ERROR")), errorText)
      addRow(3, JLabel(config.getString("ENTRY_DIRECTORY_ID")), directoryText)
      addRow(4, JLabel(config.getString("ENTRY_ID")), idText)
      parent?.revalidate()
      parent?.repaint()
    }
  }

  private fun addRow(rowIndex: Int, keyComponent: JComponent, valueComponent: JComponent) {
    var gbc = GridBagConstraints()
    gbc.gridx = 0
    gbc.gridy = rowIndex
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.insets = Insets(4, 10, 4, 3)
    add(keyComponent, gbc)

    gbc = GridBagConstraints()
    gbc.gridx = 1
    gbc.gridy = rowIndex
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.weightx = 1.0
    gbc.insets = Insets(4, 3, 4, 10)
    add(valueComponent, gbc)
  }

  private fun addKeyValue(rowIndex: Int, key: String, value: String) {
    val keyLabel = JLabel(key)
    keyLabel.font = config.defaultFont

    val valueText = JTextArea(value)
    valueText.font = config.defaultFont
    valueText.isEditable = false

    addRow(rowIndex, keyLabel, valueText)
  }

  private fun addPassword(rowIndex: Int, value: String) {
    val keyLabel = JLabel(config.getString("Password"))
    keyLabel.font = config.defaultFont

    val valueText = PasswordLabel(config, value)

    addRow(rowIndex, keyLabel, valueText)
  }
}
