package com.giyeok.passzero2.gui.entries

import com.giyeok.passzero2.core.StorageProto
import com.giyeok.passzero2.core.storage.StorageSession
import com.giyeok.passzero2.gui.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.concurrent.ExecutorService
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.ListCellRenderer
import javax.swing.SwingUtilities

class EntryDetailEditView(
  private val config: Config,
  private val session: StorageSession,
  val entry: StorageProto.Entry?,
) : JPanel() {
  private val state = EntryDetailEditViewState(
    config.executors,
    MutableStateFlow(null),
    entry?.info?.name ?: "",
    entry?.info?.type ?: StorageProto.EntryType.ENTRY_TYPE_UNSPECIFIED,
    mutableListOf()
  )
  private val stateMutex = Mutex()
  private val stateFlow = MutableSharedFlow<EntryDetailEditViewState>()
  val readyState: Flow<Boolean> = stateFlow.map { state ->
    state.initialDetails.value != null &&
      state.entryName.isNotBlank() &&
      state.entryType != StorageProto.EntryType.ENTRY_TYPE_UNSPECIFIED &&
      state.detailItems.any { !it.deleted }
  }

  init {
    initView(state)
    CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
      stateFlow.collectLatest { state ->
        updateView(state)
      }
    }
    CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
      if (entry == null) {
        setState {
          runBlocking {
            state.initialDetails.emit(StorageProto.EntryDetail.getDefaultInstance())
          }
        }
      } else {
        val initialDetails = session.getEntryDetail(entry.directory, entry.id)
        setState {
          runBlocking {
            state.initialDetails.emit(initialDetails)
          }
          state.detailItems.addAll(initialDetails.itemsList.map { item ->
            EditingEntryDetailItem(item.type, item.value, false)
          })
        }
      }
    }
  }

  private fun setState(stateUpdater: EntryDetailEditViewState.() -> Unit) {
    runBlocking {
      stateMutex.withLock {
        stateUpdater(state)
      }
      stateFlow.emit(state)
    }
  }

  lateinit var nameField: JTextField
  lateinit var entryTypeCombo: JComboBox<StorageProto.EntryType>
  lateinit var newItemButton: JButton
  lateinit var loadingLabel: JLabel
  private val items = mutableListOf<EntryDetailItemEdit>()

  private fun initView(state: EntryDetailEditViewState) {
    SwingUtilities.invokeLater {
      layout = GridBagLayout()

      var gbc = GridBagConstraints()
      gbc.gridx = 0
      gbc.gridy = 0
      val nameLabel = JLabel(config.getString("ENTRY_NAME"))
      nameLabel.font = config.defaultFont
      add(nameLabel, gbc)

      gbc = GridBagConstraints()
      gbc.gridx = 1
      gbc.gridy = 0
      gbc.fill = GridBagConstraints.HORIZONTAL
      gbc.weightx = 1.0
      nameField = JTextField()
      nameField.addKeyListener(object : KeyAdapter() {
        override fun keyReleased(e: KeyEvent?) {
          setState {
            state.entryName = nameField.text
          }
        }
      })
      nameField.font = config.defaultFont
      add(nameField, gbc)

      gbc = GridBagConstraints()
      gbc.gridx = 0
      gbc.gridy = 1
      val typeLabel = JLabel(config.getString("ENTRY_TYPE"))
      typeLabel.font = config.defaultFont
      add(typeLabel, gbc)

      gbc = GridBagConstraints()
      gbc.gridx = 1
      gbc.gridy = 1
      gbc.fill = GridBagConstraints.HORIZONTAL
      gbc.weightx = 1.0
      entryTypeCombo = JComboBox(StorageProto.EntryType.values().dropLast(1).toTypedArray())
      entryTypeCombo.renderer = ListCellRenderer { _, value, _, isSelected, _ ->
        val label =
          JLabel(config.getString((value ?: StorageProto.EntryType.ENTRY_TYPE_UNSPECIFIED).name))
        label.font = config.defaultFont
        if (isSelected) {
          label.border = BorderFactory.createLineBorder(Color.RED)
        }
        label
      }
      entryTypeCombo.addItemListener {
        setState {
          state.entryType = entryTypeCombo.selectedItem as StorageProto.EntryType
        }
      }
      entryTypeCombo.font = config.defaultFont
      add(entryTypeCombo, gbc)

      gbc = GridBagConstraints()
      gbc.gridx = 1
      gbc.gridy = 2
      gbc.fill = GridBagConstraints.HORIZONTAL
      newItemButton = JButton(config.getString("ENTRY_DETAIL_NEW_ITEM"))
      newItemButton.font = config.defaultFont
      newItemButton.addActionListener {
        setState {
          state.detailItems.add(
            EditingEntryDetailItem(
              StorageProto.EntryDetailItemType.ENTRY_DETAIL_ITEM_UNKNOWN,
              "",
              false
            )
          )
        }
      }
      add(newItemButton, gbc)

      gbc = GridBagConstraints()
      gbc.gridx = 0
      gbc.gridy = 3
      loadingLabel = JLabel(config.getString("LOADING"))
      add(loadingLabel, gbc)

      updateView(state)
    }
  }

  private fun updateNewItemButtonLayout() {
    val constraints = (layout as GridBagLayout).getConstraints(newItemButton)
    constraints.gridy = state.detailItems.size + 2
    (layout as GridBagLayout).setConstraints(newItemButton, constraints)
  }

  private fun setEditingEnabled(desired: Boolean) {
    if (nameField.isEnabled != desired) {
      nameField.isEnabled = desired
    }
    if (entryTypeCombo.isEnabled != desired) {
      entryTypeCombo.isEnabled = desired
    }
    if (newItemButton.isEnabled != desired) {
      newItemButton.isEnabled = desired
    }
    items.forEach {
      if (it.typeCombo.isEnabled != desired) {
        it.typeCombo.isEnabled = desired
      }
      if (it.textField.isEnabled != desired) {
        it.textField.isEnabled = desired
      }
      if (it.deleteCheck.isEnabled != desired) {
        it.deleteCheck.isEnabled = desired
      }
    }
  }

  private fun updateView(state: EntryDetailEditViewState) {
    SwingUtilities.invokeLater {
      if (state.initialDetails.value == null) {
        // initial detail 로딩중. 편집 기능 disable
        loadingLabel.isVisible = true
        setEditingEnabled(false)
      } else {
        // initial detail 로딩 완료. 편집 기능 enable
        loadingLabel.isVisible = false
        setEditingEnabled(true)
      }
      if (nameField.text != state.entryName) {
        nameField.text = state.entryName
      }
      if (entryTypeCombo.selectedItem != state.entryType) {
        entryTypeCombo.selectedItem = state.entryType
      }

      items.forEachIndexed { index, item ->
        item.item = state.detailItems[index]
        if (item.typeCombo.selectedItem != item.item.itemType) {
          item.typeCombo.selectedItem = item.item.itemType
        }
        if (item.textField.text != item.item.value) {
          item.textField.text = item.item.value
        }
        if (item.deleteCheck.isSelected != item.item.deleted) {
          item.deleteCheck.isSelected = item.item.deleted
        }
      }

      (items.size until state.detailItems.size).forEach { index ->
        val item = state.detailItems[index]
        val gridy = index + 2

        var gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = gridy
        val itemTypeCombo =
          JComboBox(StorageProto.EntryDetailItemType.values().dropLast(1).toTypedArray())
        itemTypeCombo.selectedItem = item.itemType
        itemTypeCombo.renderer = ListCellRenderer { _, value, _, isSelected, _ ->
          val valueName = (value ?: StorageProto.EntryDetailItemType.ENTRY_DETAIL_ITEM_UNKNOWN).name
          val label = JLabel(config.getString(valueName))
          label.font = config.defaultFont
          if (isSelected) {
            label.border = BorderFactory.createLineBorder(Color.RED)
          }
          label
        }
        itemTypeCombo.addActionListener {
          setState {
            item.itemType = itemTypeCombo.selectedItem as StorageProto.EntryDetailItemType
          }
        }
        add(itemTypeCombo, gbc)

        gbc = GridBagConstraints()
        gbc.gridx = 1
        gbc.gridy = gridy
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        val textField = JTextField(item.value)
        textField.font = config.defaultFont
        textField.addKeyListener(object : KeyAdapter() {
          override fun keyReleased(e: KeyEvent?) {
            setState {
              item.value = textField.text
            }
          }
        })
        add(textField, gbc)

        gbc = GridBagConstraints()
        gbc.gridx = 2
        gbc.gridy = gridy
        val deleteCheck = JCheckBox("-", item.deleted)
        deleteCheck.addChangeListener {
          setState {
            item.deleted = deleteCheck.isSelected
          }
        }
        add(deleteCheck, gbc)

        items.add(EntryDetailItemEdit(index, item, itemTypeCombo, textField, deleteCheck))
      }
      updateNewItemButtonLayout()
      revalidate()
      repaint()
    }
  }

  fun getEntryInfo(): StorageProto.EntryInfo {
    return StorageProto.EntryInfo.newBuilder()
      .setName(state.entryName)
      .setType(state.entryType)
      .build()
  }

  fun getEntryDetails(): StorageProto.EntryDetail {
    val detailItems = state.detailItems.filterNot { it.deleted }.map { item ->
      StorageProto.EntryDetailItem.newBuilder()
        .setType(item.itemType)
        .setValue(item.value)
        .build()
    }
    return StorageProto.EntryDetail.newBuilder().addAllItems(detailItems).build()
  }
}

class EntryDetailItemEdit(
  var index: Int,
  var item: EditingEntryDetailItem,
  val typeCombo: JComboBox<StorageProto.EntryDetailItemType>,
  val textField: JTextField,
  val deleteCheck: JCheckBox,
)

data class EntryDetailEditViewState(
  val executors: ExecutorService,
  val initialDetails: MutableStateFlow<StorageProto.EntryDetail?>,
  var entryName: String,
  var entryType: StorageProto.EntryType,
  val detailItems: MutableList<EditingEntryDetailItem>,
)

data class EditingEntryDetailItem(
  var itemType: StorageProto.EntryDetailItemType,
  var value: String,
  var deleted: Boolean,
)
