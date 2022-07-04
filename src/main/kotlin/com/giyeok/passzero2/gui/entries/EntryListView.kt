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
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class EntryListView(
  private val config: Config,
  private val session: StorageSession,
  initialDirectory: String
) : JPanel() {
  private val state = EntryListViewState(
    config.executors,
    MutableStateFlow(null),
    DefaultComboBoxModel(),
    initialDirectory,
    false,
    MutableStateFlow(null),
    mutableListOf(),
    "",
    DefaultListModel(),
    EntryDetailState.EmptyDetails(true)
  )
  private val stateMutex = Mutex()
  private val stateFlow = MutableSharedFlow<EntryListViewState>()

  private val directoryCombo = JComboBox(state.directoryList)
  private val refreshButton = JButton(ImageIcon(javaClass.getResource("/icons8-refresh-30.png")))

  private val list = JList(state.filteredEntries)
  private val searchTextField = JTextField()
  private val detailScroll = JScrollPane()
  private val detailMenus = JPanel()

  init {
    initView(state)
    CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
      stateFlow.map { it.entryDetailState }.distinctUntilChanged().collectLatest { state ->
        updateDetailsView(state)
      }
    }
    CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
      stateFlow.map { it.regeneratingCache }.distinctUntilChanged()
        .collectLatest { regeneratingCache ->
          SwingUtilities.invokeLater {
            refreshButton.isEnabled = !regeneratingCache
          }
        }
    }
  }

  private fun setState(stateUpdater: EntryListViewState.() -> Unit) {
    runBlocking {
      stateMutex.withLock {
        stateUpdater(state)
      }
      stateFlow.emit(state)
    }
  }

  private fun initView(state: EntryListViewState) {
    layout = BorderLayout()

    directoryCombo.font = config.bigFont
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
      when (state.entryDetailState) {
        is EntryDetailState.EditingEntry, is EntryDetailState.CreatingEntry -> {
        }
        is EntryDetailState.ShowingDetails, is EntryDetailState.EmptyDetails -> {
          if (!event.valueIsAdjusting) {
            if (list.selectedIndex >= 0 && list.selectedIndex < state.filteredEntries.size) {
              val entry = state.filteredEntries[list.selectedIndex]
              setState {
                state.entryDetailState = EntryDetailState.ShowingDetails(entry, true)
              }
            } else {
              setState {
                state.entryDetailState = EntryDetailState.EmptyDetails(true)
              }
            }
          }
        }
      }
    }

    val listScroll = JScrollPane()
    listScroll.setViewportView(list)

    val leftTopPanel = JPanel()
    leftTopPanel.layout = GridBagLayout()
    val sessionInfoButton = JButton(ImageIcon(javaClass.getResource("/icons8-info-30.png")))
    var gbc = GridBagConstraints()
    gbc.gridx = 0
    gbc.gridy = 0
    sessionInfoButton.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        super.mouseClicked(e)
      }
    })
    leftTopPanel.add(sessionInfoButton, gbc)

    gbc = GridBagConstraints()
    gbc.gridx = 1
    gbc.gridy = 0
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.weightx = 1.0
    leftTopPanel.add(directoryCombo, gbc)

    gbc = GridBagConstraints()
    gbc.gridx = 2
    gbc.gridy = 0
    refreshButton.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (!state.regeneratingCache) {
          if (e.clickCount >= 2) {
            setState {
              state.regeneratingCache = true
              CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
                val stream = session.createEntryListCacheStreaming(directory).onCompletion {
                  setState {
                    state.regeneratingCache = false
                  }
                }
                state.emitEntryListUpdater(stream)
              }
            }
          } else {
            // TODO 캐시만 새로 읽도록 수정
          }
        }
      }
    })
    leftTopPanel.add(refreshButton, gbc)

    val leftBottomPanel = JPanel()
    leftBottomPanel.layout = GridBagLayout()

    searchTextField.font = config.defaultFont
    gbc = GridBagConstraints()
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.weightx = 1.0
    searchTextField.addKeyListener(object : KeyListener {
      override fun keyTyped(e: KeyEvent?) {
      }

      override fun keyPressed(e: KeyEvent?) {
      }

      override fun keyReleased(e: KeyEvent?) {
        setState {
          state.filterText = searchTextField.text
        }
      }
    })
    leftBottomPanel.add(searchTextField, gbc)

    val newSheetButton = JButton(config.getString("NEW_ENTRY"))
    newSheetButton.font = config.defaultFont
    gbc = GridBagConstraints()
    gbc.gridx = 1
    gbc.gridy = 0
    newSheetButton.addActionListener {
      setState {
        state.entryDetailState = EntryDetailState.CreatingEntry(list.selectedValue)
      }
    }
    leftBottomPanel.add(newSheetButton, gbc)

    val leftPanel = JPanel()
    leftPanel.layout = BorderLayout()
    leftPanel.add(leftTopPanel, BorderLayout.NORTH)
    leftPanel.add(listScroll, BorderLayout.CENTER)
    leftPanel.add(leftBottomPanel, BorderLayout.SOUTH)

    detailMenus.layout = FlowLayout(FlowLayout.TRAILING)

    val rightPanel = JPanel()
    rightPanel.layout = BorderLayout()
    rightPanel.add(detailScroll, BorderLayout.CENTER)
    rightPanel.add(detailMenus, BorderLayout.SOUTH)

    val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel)
    splitPane.dividerLocation = 400
    splitPane.dividerSize = 5
    splitPane.resizeWeight = 0.5
    add(splitPane, BorderLayout.CENTER)

    setState {
      state.emitDirectoryListUpdater(session.streamDirectoryList())

      CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
        val cached = session.getEntryListCache(directory)
        if (cached != null) {
          setState {
            state.emitEntryListUpdater(flowOf(*cached.entriesList.toTypedArray()))
          }
        } else {
          setState {
            CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
              val flow = session.createEntryListCacheStreaming(state.directory)
              state.emitEntryListUpdater(flow)
            }
          }
        }
      }
    }
  }

  private fun updateDetailsView(detailState: EntryDetailState) {
    fun initEditView(
      entry: StorageProto.Entry?,
      saveButtonString: String,
      cancelButtonString: String,
      selectionAfterCancel: StorageProto.Entry?,
      applyFunc: suspend (StorageProto.EntryInfo, StorageProto.EntryDetail) -> Unit
    ) {
      val editView = EntryDetailEditView(config, session, entry)
      detailScroll.setViewportView(editView)

      val saveButton = JButton(config.getString(saveButtonString))
      saveButton.isEnabled = false

      CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
        editView.readyState.collect { ready ->
          SwingUtilities.invokeLater {
            saveButton.isEnabled = ready
          }
        }
      }
      saveButton.addActionListener {
        val entryInfo = editView.getEntryInfo()
        val details = editView.getEntryDetails()
        CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
          applyFunc(entryInfo, details)
        }
      }

      val cancelButton = JButton(config.getString(cancelButtonString))
      cancelButton.addActionListener {
        setState {
          this.entryDetailState = if (selectionAfterCancel == null) {
            EntryDetailState.EmptyDetails(false)
          } else {
            EntryDetailState.ShowingDetails(selectionAfterCancel, false)
          }
        }
      }

      detailMenus.removeAll()
      detailMenus.isVisible = true
      detailMenus.add(saveButton)
      detailMenus.add(cancelButton)
    }

    SwingUtilities.invokeLater {
      when (detailState) {
        is EntryDetailState.CreatingEntry -> {
          list.isEnabled = false
          list.clearSelection()

          initEditView(
            null,
            "ENTRY_CREATE_SAVE",
            "ENTRY_CREATE_CANCEL",
            detailState.lastSelection
          ) { entryInfo, details ->
            val newEntry = session.createEntry(state.directory, entryInfo, details)

            setState {
              this.addEntry(newEntry)
              this.entryDetailState = if (detailState.lastSelection != null) {
                EntryDetailState.ShowingDetails(detailState.lastSelection, false)
              } else {
                EntryDetailState.EmptyDetails(false)
              }
            }
          }
        }
        is EntryDetailState.EditingEntry -> {
          list.isEnabled = false
          list.setSelectedValue(detailState.entry, true)

          initEditView(
            null,
            "ENTRY_EDIT_SAVE",
            "ENTRY_EDIT_CANCEL",
            detailState.entry
          ) { entryInfo, details ->
            session.updateEntry(state.directory, detailState.entry.id, entryInfo, details)

            setState {
              this.entryDetailState = EntryDetailState.ShowingDetails(detailState.entry, false)
            }
          }
        }
        is EntryDetailState.ShowingDetails -> {
          list.isEnabled = true
          if (!detailState.userTriggered) {
            SwingUtilities.invokeLater {
              list.setSelectedValue(detailState.entry, true)
            }
          }

          detailScroll.setViewportView(EntryDetailView(config, session, detailState.entry))

          detailMenus.removeAll()
          detailMenus.isVisible = true

          val editButton = JButton(config.getString("ENTRY_EDIT"))
          editButton.addActionListener {
            setState {
              this.entryDetailState = EntryDetailState.EditingEntry(detailState.entry)
            }
          }
          detailMenus.add(editButton)

          val deleteButton = JButton(config.getString("ENTRY_DELETE"))
          deleteButton.addActionListener {
            // TODO 엔트리 삭제 처리
            setState {
              this.entryDetailState = EntryDetailState.EmptyDetails(false)
            }
          }
          detailMenus.add(deleteButton)
        }
        is EntryDetailState.EmptyDetails -> {
          list.isEnabled = true
          if (!detailState.userTriggered) {
            SwingUtilities.invokeLater {
              list.clearSelection()
            }
          }

          detailScroll.setViewportView(JLabel(config.getString("SELECT_ENTRY")))
          detailMenus.removeAll()
          detailMenus.isVisible = false
        }
      }
      detailMenus.revalidate()
      detailMenus.repaint()
    }
  }
}
