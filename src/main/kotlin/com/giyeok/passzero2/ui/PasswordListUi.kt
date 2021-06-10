package com.giyeok.passzero2.ui

import com.giyeok.passzero2.core.Session
import com.giyeok.passzero2.core.SessionSecret
// import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.exceptions.CompositeException
import io.reactivex.schedulers.Schedulers
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DataFormat
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.text.TextAlignment
import javafx.util.Callback
import javafx.util.Duration
import java.util.concurrent.TimeUnit

class PasswordListController {
    @FXML
    lateinit var sheetList: ListView<SheetListItem>
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

data class SheetListItem(val sheetId: String, val meta: Session.SheetMeta)

enum class SortScheme(val comparator: Comparator<SheetListItem>) {
    NameLexicographically(Comparator { a, b -> a.meta.name.compareTo(b.meta.name, true) }),
    CreatedTime(Comparator { a, b -> a.meta.createdTime.compareTo(b.meta.createdTime) }),
    ModifiedTime(Comparator { a, b -> a.meta.modifiedTime.compareTo(b.meta.modifiedTime) })
}

class PasswordListUi(private val main: UiMain, sessionSecret: SessionSecret, private val scheduler: Scheduler) {
    private lateinit var view: Pane
    private lateinit var controller: PasswordListController
    private val session = sessionSecret.createSession()

    class SheetItem(var meta: Session.SheetMeta, var detail: Session.SheetDetail?, var listItem: SheetListItem)

    private val sheetStore = mutableMapOf<String, SheetItem>()
    private val sheetsList = FXCollections.observableArrayList<SheetListItem>()

    // sheet meta들에서 사용된 적이 있는 모든 tag들의 집합.
    // 만약 sheet에서 해당 태그가 지워지거나 해당 sheet가 지워지더라도 현재 세션에서 tag가 지워지진 않는다.
    // -> 나중에 필요하면 만들 것
    private val tagStore = mutableSetOf<String>()

    fun viewRoot(): Parent {
        val loader = FXMLLoader(javaClass.getResource("/views/passwordList2.fxml"))
        view = loader.load()
        controller = loader.getController()

        return view
    }

    fun addTags(tags: List<String>) {
        synchronized(tagStore) {
            tagStore.addAll(tags)
        }
    }

    fun addSheet(sheetId: String, meta: Session.SheetMeta, detail: Session.SheetDetail?) {
        addTags(meta.tags)
        synchronized(sheetStore) {
            val existing = sheetStore[sheetId]
            if (existing == null) {
                val listItem = SheetListItem(sheetId, meta)
                sheetStore[sheetId] = SheetItem(meta, detail, listItem)
                // add to view
                Platform.runLater {
                    sheetsList.add(listItem)
                }
            } else {
                if (existing.meta.version < meta.version) {
                    // 새로운 meta가 더 최신인 경우
                    existing.meta = meta
                    if (existing.meta.name != meta.name) {
                        // update sheet name
                        Platform.runLater {
                            val selectedItem = (controller.sheetList.selectionModel.selectedItem == existing.listItem)
                            sheetsList.remove(existing.listItem)
                            sheetsList.add(existing.listItem)
                            if (selectedItem) {
                                controller.sheetList.selectionModel.select(existing.listItem)
                            }
                        }
                    }
                }
                val existingDetail = existing.detail
                if (detail != null && (existingDetail == null || existingDetail.version < detail.version)) {
                    existing.detail = detail
                    // TODO update detail view if needed
                }
            }
            0
        }
    }

    fun start() {
        initUi()
        session.start().subscribeOn(scheduler).subscribe {
            session.firstInfo()
                    .doOnError { error ->
                        // TODO do something
                    }
                    .doOnSuccess { firstInfo ->
                        firstInfo.cachedSheets.forEach { addSheet(it.id, it.meta, it.detail) }
                        addTags(firstInfo.cachedTags)
                    }
                    .subscribeOn(scheduler)
                    .subscribe()

            session.sheetMetas()
                    .subscribeOn(scheduler)
                    .subscribe(
                            { (sheetId, meta) ->
                                println("$sheetId, $meta")
                                addSheet(sheetId, meta, null)
                            },
                            { error ->
                                println("Error while fetching sheet metas: $error")
                                if (error is CompositeException) {
                                    error.exceptions.forEach { println(it) }
                                }
                                sortList()
                            },
                            {
                                println("Completed fetching sheet metas")
                                sortList()
                            }
                    )
        }
    }

    inner class LabelListCell : Callback<ListView<SheetListItem>, ListCell<SheetListItem>> {
        override fun call(param: ListView<SheetListItem>?): ListCell<SheetListItem> {
            return object : ListCell<SheetListItem>() {
                override fun updateItem(item: SheetListItem?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (item != null) {
                        this.text = item.meta.name
                    }
                }
            }
        }
    }

    private fun sortList() {
        Platform.runLater {
            sheetsList.sortWith(controller.sheetListSortScheme.selectionModel.selectedItem.comparator)
        }
    }

    private val clipboard = Clipboard.getSystemClipboard()

    private fun copyToClipboard(value: String, timeout: Duration) {
        Platform.runLater {
            val clipboardContent = ClipboardContent()
            clipboardContent.putString(value)
            clipboard.setContent(clipboardContent)
        }
        if (timeout != Duration.INDEFINITE) {
            Observable.timer(timeout.toMillis().toLong(), TimeUnit.MILLISECONDS, Schedulers.io()).subscribe {
                Platform.runLater {
                    val currentClipboard = clipboard.getContent(DataFormat.PLAIN_TEXT)
                    if (currentClipboard == value) {
                        clipboard.clear()
                        println("Clipboard cleared")
                    } else {
                        println("Preserving clipboard")
                    }
                }
            }
        }
    }

    private fun renderSheetDetail(sheetId: String, meta: Session.SheetMeta, detail: Session.SheetDetail) {
        Platform.runLater {
            val gridPane = GridPane()
            gridPane.hgap = 5.0
            gridPane.vgap = 3.0

            class GridGenerator(private val gridPane: GridPane) {
                private var rowNum = 0

                fun addRow(name: String, rhs: Node) {
                    val lhs = Label(name)
                    lhs.textAlignment = TextAlignment.RIGHT
                    lhs.minWidth = 80.0
                    gridPane.add(lhs, 0, rowNum)
                    gridPane.add(rhs, 1, rowNum)
                    rowNum += 1
                }
            }

            val g = GridGenerator(gridPane)

            g.addRow("Name:", Label(meta.name))
            g.addRow("Type:", Label(meta.sheetType.code))
            g.addRow("Version:", Label("${meta.version}"))
            g.addRow("Tags:", Label(meta.tags.joinToString(", ")))
            g.addRow("Created:", Label(meta.createdTime.toString()))
            g.addRow("Modified:", Label(meta.modifiedTime.toString()))

            g.addRow("Detail Version:", Label("${detail.version}"))
            detail.fields.forEach { field ->
                fun labelWithCopy(text: String, value: String, duration: Duration): Node {
                    val label = Label(text)
                    val copyButton = Button("Copy")
                    copyButton.setOnAction {
                        copyToClipboard(value, duration)
                    }
                    return HBox(copyButton, label)
                }

                fun labelWithGoto(uri: String): Node {
                    val link = Hyperlink(uri)
                    link.setOnAction {
                        Platform.runLater {
                            // HostServicesFactory.getInstance(main).showDocument(uri)
                        }
                    }
                    return link
                }

                val rhs: Node = when (field.type) {
                    Session.FieldType.Username -> labelWithCopy(field.value, field.value, Duration.seconds(60.0))
                    Session.FieldType.Password -> labelWithCopy("****", field.value, Duration.seconds(30.0))
                    Session.FieldType.Website -> labelWithGoto(field.value)
                    else -> Label(field.value)
                }
                g.addRow(field.type.code, rhs)
            }

            controller.sheetContentHolder.content = gridPane
        }
    }

    private fun showSheetDetail(sheetId: String) {
        val (cachedMeta, cachedDetail) = synchronized(sheetStore) {
            Pair(sheetStore[sheetId]?.meta, sheetStore[sheetId]?.detail)
        }
        if (cachedMeta != null && cachedDetail != null) {
            renderSheetDetail(sheetId, cachedMeta, cachedDetail)
        } else {
            session.sheet(sheetId)
                    .subscribeOn(scheduler)
                    .subscribe(
                            { sheet ->
                                addSheet(sheet.id, sheet.meta, sheet.detail)
                                renderSheetDetail(sheet.id, sheet.meta, sheet.detail)
                            },
                            { error ->
                                TODO()
                            })
        }
    }

    private fun clearSheetDetail() {
        Platform.runLater {
            controller.sheetContentHolder.content = Label("")
        }
    }

    private fun initUi() {
        Platform.runLater {
            controller.sheetListSortScheme.items.addAll(SortScheme.values())
            controller.sheetListSortScheme.selectionModel.select(0)
            controller.sheetListSortScheme.selectionModel.selectedIndexProperty()!!.addListener { _ -> sortList() }

            controller.sheetList.cellFactory = LabelListCell()
            controller.sheetList.items = sheetsList

            controller.sheetList.selectionModel.selectedItemProperty()!!.addListener { observable, oldValue, newValue ->
                if (newValue == null) {
                    clearSheetDetail()
                } else {
                    showSheetDetail(newValue.sheetId)
                }
            }

            controller.sheetMenuHolder.children.clear()
        }
    }
}
