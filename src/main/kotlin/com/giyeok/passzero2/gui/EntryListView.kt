package com.giyeok.passzero2.gui

import com.giyeok.passzero2.core.StorageProto
import com.giyeok.passzero2.core.storage.StorageSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import javax.swing.*

class EntryListView(
  private val config: Config,
  private val session: StorageSession,
  initialDirectory: String
) : JPanel() {
  private val leftPanel = JPanel()
  private val directoryListModel = DefaultComboBoxModel<StorageProto.DirectoryInfo>()
  private val directoryCombo = JComboBox(directoryListModel)
  private val listModel = DefaultListModel<StorageProto.Entry>()
  private var directoryUpdateJob: Job? = null
  private val listScroll = JScrollPane()
  private val list = JList(listModel)
  private val detailScroll = JScrollPane()
  private val detail = EntryDetailView(config)

  var directory: String = initialDirectory
    set(newDirectory) {
      field = newDirectory
      updateEntryList()
    }

  private fun updateDirectoryList() {
    config.coroutineScope.launch {
      session.streamDirectoryList().collect { directory ->
        directoryListModel.addElement(directory)
      }
    }
  }

  private fun updateEntryList() = synchronized(this) {
    directoryUpdateJob?.cancel()
    directoryUpdateJob = null
    SwingUtilities.invokeLater {
      listModel.clear()
    }
    directoryUpdateJob = config.coroutineScope.launch {
      val cached = session.getEntryListCache(directory)
      if (cached != null) {
        listModel.addAll(cached.entriesList.sortedBy { it.info.name.lowercase() })
      } else {
        session.streamEntryList(directory).collect { entry ->
          if (isActive) {
            SwingUtilities.invokeLater {
              addEntry(entry)
            }
          }
        }
        directoryUpdateJob = null
      }
    }
  }

  private fun addEntry(entry: StorageProto.Entry) = synchronized(this) {
    if (entry.directory == directory) {
      var index = 0
      // TODO binary search?
      while (index < listModel.size && listModel[index].info.name.compareTo(entry.info.name, true) < 0) {
        index += 1
      }
      listModel.add(index, entry)
    }
  }

  init {
    layout = GridLayout(1, 2)

    leftPanel.layout = BorderLayout()

    directoryCombo.renderer = ListCellRenderer { _, value, _, _, _ ->
      JLabel(value?.name ?: "???")
    }

    list.cellRenderer = ListCellRenderer { _, value, _, isSelected, _ ->
      val label = JLabel(value.info.name)
      label.font = config.defaultFont
      if (isSelected) {
        label.foreground = Color.BLACK
        label.background = Color.LIGHT_GRAY
        label.isOpaque = true
        label.border = BorderFactory.createLineBorder(Color.RED)
      } else {
        label.border = BorderFactory.createLineBorder(Color.WHITE)
      }
      label
    }
    list.layoutOrientation = JList.VERTICAL

    list.selectionMode = ListSelectionModel.SINGLE_SELECTION
    list.addListSelectionListener { event ->
      if (!event.valueIsAdjusting && list.selectedIndex >= 0 && list.selectedIndex < listModel.size) {
        val entry = listModel[list.selectedIndex]
        detail.entry = entry
        detail.entryDetail = null
        config.coroutineScope.launch {
          val entryDetail = session.getEntryDetail(entry.directory, entry.id)
          detail.entryDetail = entryDetail
        }
      } else {
        detail.entry = null
        detail.entryDetail = null
      }
    }

    listScroll.setViewportView(list)
    leftPanel.add(directoryCombo, BorderLayout.NORTH)
    leftPanel.add(listScroll, BorderLayout.CENTER)
    add(leftPanel)
    detailScroll.setViewportView(detail)
    add(detailScroll)

    updateDirectoryList()
    updateEntryList()
  }
}

class EntryDetailView(private val config: Config) : JPanel() {
  private val gridLayout = GridBagLayout()

  init {
    layout = gridLayout
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

  var entry: StorageProto.Entry? = null
    set(newEntry) {
      field = newEntry
      if (newEntry == null) {
        SwingUtilities.invokeLater {
          removeAll()
          parent.revalidate()
          parent.repaint()
        }
      } else {
        SwingUtilities.invokeLater {
          addKeyValue(0, config.getString("Title"), newEntry.info.name)
          addKeyValue(1, config.getString("Type"), config.getString(newEntry.info.type.toString()))
          parent.revalidate()
          parent.repaint()
        }
      }
    }
  var entryDetail: StorageProto.EntryDetail? = null
    set(newDetail) {
      field = newDetail
      SwingUtilities.invokeLater {
        newDetail?.itemsList?.forEachIndexed { index, item ->
          when (item.type) {
            StorageProto.EntryDetailItemType.ENTRY_DETAIL_ITEM_PASSWORD -> addPassword(index + 2, item.value)
            else -> addKeyValue(index + 2, config.getString(item.type.toString()), item.value)
          }
        }
        parent.revalidate()
        parent.repaint()
      }
    }
}

class PasswordLabel(config: Config, private val password: String) : JPanel() {
  private val passwordArea = JTextArea()
  private var showingPassword = false

  init {
    layout = GridBagLayout()

    passwordArea.isEditable = false
    passwordArea.text = "****"
    passwordArea.isEnabled = false
    passwordArea.font = config.defaultFont
    var gbc = GridBagConstraints()
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.weightx = 1.0
    gbc.fill = GridBagConstraints.HORIZONTAL
    add(passwordArea, gbc)

    val showButton = JButton()
    showButton.text = "Show"
    showButton.font = config.defaultFont

    showButton.addActionListener {
      showingPassword = !showingPassword
      if (showingPassword) {
        passwordArea.text = password
        passwordArea.isEnabled = true
      } else {
        passwordArea.text = "****"
        passwordArea.isEnabled = false
      }
    }

    gbc = GridBagConstraints()
    gbc.gridx = 1
    gbc.gridy = 0
    add(showButton, gbc)

    val copyButton = JButton()
    copyButton.text = "Copy"
    copyButton.font = config.defaultFont

    copyButton.addActionListener {
      val clipboard = Toolkit.getDefaultToolkit().systemClipboard
      val copied = StringSelection(password)
      SwingUtilities.invokeLater {
        clipboard.setContents(copied, null)
      }
      config.coroutineScope.launch {
        delay(30 * 1000) // 30 secs
        SwingUtilities.invokeLater {
          val current = clipboard.getContents(null)
          if (current?.getTransferData(DataFlavor.stringFlavor) == password) {
            clipboard.setContents(object : Transferable {
              override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf()
              override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean = false
              override fun getTransferData(flavor: DataFlavor?): Any = TODO()
            }, null)
          }
        }
      }
    }

    gbc = GridBagConstraints()
    gbc.gridx = 2
    gbc.gridy = 0
    add(copyButton, gbc)
  }
}
