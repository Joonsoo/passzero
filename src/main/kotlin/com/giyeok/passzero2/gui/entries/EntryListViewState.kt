package com.giyeok.passzero2.gui.entries

import com.giyeok.passzero2.core.StorageProto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import java.util.concurrent.ExecutorService
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListModel
import javax.swing.SwingUtilities

data class EntryListViewState(
  val executors: ExecutorService,
  val directoryListUpdaters: MutableStateFlow<Flow<StorageProto.DirectoryInfo>?>,
  val directoryList: DefaultComboBoxModel<StorageProto.DirectoryInfo>,
  private var _directory: String,
  var regeneratingCache: Boolean,
  val entryListUpdaters: MutableStateFlow<Flow<StorageProto.Entry>?>,
  val entryList: MutableList<StorageProto.Entry>,
  private var _filterText: String,
  val filteredEntries: DefaultListModel<StorageProto.Entry>,
  var entryDetailState: EntryDetailState,
) {
  init {
    CoroutineScope(executors.asCoroutineDispatcher()).launch {
      directoryListUpdaters.filterNotNull().collectLatest { updater ->
        SwingUtilities.invokeLater {
          directoryList.removeAllElements()
        }
        updater.collect { newDirectory ->
          SwingUtilities.invokeLater {
            directoryList.addElement(newDirectory)
            if (_directory == newDirectory.id) {
              directoryList.selectedItem = newDirectory
            }
          }
        }
      }
    }

    CoroutineScope(executors.asCoroutineDispatcher()).launch {
      entryListUpdaters.filterNotNull().collectLatest { updater ->
        entryList.clear()
        SwingUtilities.invokeLater {
          filteredEntries.clear()
        }
        updater.collect { newEntry ->
          addEntry(newEntry)
        }
      }
    }
  }

  fun isPassingFilter(entry: StorageProto.Entry) =
    filterText.isEmpty() || entry.info.name.indexOf(filterText, ignoreCase = true) >= 0

  fun addEntry(entry: StorageProto.Entry) {
    var listIndex = 0
    // TODO binary search?
    while (
      listIndex < entryList.size &&
      entryList[listIndex].info.name.compareTo(entry.info.name, true) < 0
    ) {
      listIndex += 1
    }
    entryList.add(listIndex, entry)
    if (isPassingFilter(entry)) {
      SwingUtilities.invokeLater {
        // TODO duplicate code. refactoring?
        var filtIndex = 0
        while (
          filtIndex < filteredEntries.size &&
          filteredEntries[filtIndex].info.name.compareTo(entry.info.name, true) < 0
        ) {
          filtIndex += 1
        }
        filteredEntries.add(filtIndex, entry)
      }
    }
  }

  fun emitDirectoryListUpdater(updater: Flow<StorageProto.DirectoryInfo>?) {
    runBlocking {
      directoryListUpdaters.emit(updater)
    }
  }

  fun emitEntryListUpdater(updater: Flow<StorageProto.Entry>?) {
    runBlocking {
      entryListUpdaters.emit(updater)
    }
  }

  var directory: String
    get() = _directory
    set(value) {
      _directory = value
      SwingUtilities.invokeLater {
        val selectedIdx =
          (0 until directoryList.size).find { directoryList.getElementAt(it).id == value }
        directoryList.selectedItem = selectedIdx?.let { directoryList.getElementAt(selectedIdx) }
      }
    }

  var filterText: String
    get() = _filterText
    set(value) {
      _filterText = value
      SwingUtilities.invokeLater {
        filteredEntries.clear()
        filteredEntries.addAll(entryList.filter { isPassingFilter(it) })
      }
    }
}

sealed class EntryDetailState {
  // active는 사용자가 클릭해서 생긴게 아니라서 list에 실제로 변경을 일으켜야 하는 경우
  data class EmptyDetails(val userTriggered: Boolean) : EntryDetailState()

  data class ShowingDetails(val entry: StorageProto.Entry, val userTriggered: Boolean) :
    EntryDetailState()

  data class EditingEntry(val entry: StorageProto.Entry) : EntryDetailState()

  data class CreatingEntry(val lastSelection: StorageProto.Entry?) : EntryDetailState()
}
