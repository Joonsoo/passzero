package com.giyeok.passzero2.ui

import com.giyeok.passzero2.core.PasswordManager
import com.giyeok.passzero2.core.Session
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane

class PasswordListController {
    @FXML
    lateinit var directoryComboBox: ComboBox<ListItem<DirectoryId, DirectoryInfo>>
    @FXML
    lateinit var sheetList: ListView<ListItem<SheetId, SheetInfo>>
    @FXML
    lateinit var sheetListSortScheme: ComboBox<SortScheme>
    @FXML
    lateinit var sheetSearchTextField: TextField
    @FXML
    lateinit var newSheetButton: Button
    @FXML
    lateinit var sheetContentHolder: ScrollPane
    @FXML
    lateinit var sheetMenuHolder: StackPane
    @FXML
    lateinit var sheetMenuViewMode: HBox
    @FXML
    lateinit var sheetMenuEditMode: HBox
    @FXML
    lateinit var sheetEditButton: Button
    @FXML
    lateinit var sheetEditSaveButton: Button
    @FXML
    lateinit var sheetEditCancelButton: Button
    @FXML
    lateinit var sheetDeleteButton: Button
}

data class ListItem<K, V>(val id: K, val value: V, val name: String)

enum class SortScheme {
    NameLexicographically
}

data class SheetId(val key: String) {
    // timestamp
}

data class SheetInfo(val key: String)

data class DirectoryId(val key: String)

data class DirectoryInfo(val info: String)

class PasswordListUi(main: UiMain, session: Session) {
    private lateinit var view: Pane
    private lateinit var controller: PasswordListController

    private val passwordManager = PasswordManager(session)

    fun viewRoot(): Parent {
        val loader = FXMLLoader(javaClass.getResource("/views/passwordList.fxml"))
        view = loader.load()
        controller = loader.getController()

        return view
    }
}
