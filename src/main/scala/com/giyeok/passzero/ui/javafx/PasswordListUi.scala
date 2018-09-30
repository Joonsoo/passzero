package com.giyeok.passzero.ui.javafx

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import com.giyeok.passzero.Password._
import com.giyeok.passzero.{Password, PasswordManager, Session}
import javafx.application.Platform
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.{FXCollections, ObservableList}
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.control.Alert.AlertType
import javafx.scene.control._
import javafx.scene.input.{Clipboard, ClipboardContent, DataFormat}
import javafx.scene.layout._
import javafx.scene.text.{Font, TextAlignment}
import javafx.scene.{Node, Parent}
import javafx.util.{Callback, StringConverter}
import org.json4s.native.JsonMethods._

import scala.collection.JavaConverters._
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class ListItem[K, V](id: K)(val value: V, val name: String)

case class SortScheme(name: String)(comparator: (ListItem[SheetId, SheetInfo], ListItem[SheetId, SheetInfo]) => Int) {
    def sort(sheetsList: ObservableList[ListItem[SheetId, SheetInfo]]): Unit = {
        sheetsList.sort((a, b) => comparator(a, b))
    }
}

class PasswordListController {
    @FXML
    var directoryComboBox: ComboBox[ListItem[DirectoryId, DirectoryInfo]] = _
    @FXML
    var sheetList: ListView[ListItem[SheetId, SheetInfo]] = _
    @FXML
    var sheetListSortScheme: ComboBox[SortScheme] = _
    @FXML
    var sheetSearchTextField: TextField = _
    @FXML
    var newSheetButton: Button = _
    @FXML
    var sheetContentHolder: ScrollPane = _
    @FXML
    var sheetMenuHolder: StackPane = _
    @FXML
    var sheetMenuViewMode: HBox = _
    @FXML
    var sheetMenuEditMode: HBox = _
    @FXML
    var sheetEditButton: Button = _
    @FXML
    var sheetEditSaveButton: Button = _
    @FXML
    var sheetEditCancelButton: Button = _
    @FXML
    var sheetDeleteButton: Button = _
}

class LabelListCell[T](converter: T => String) extends Callback[ListView[T], ListCell[T]] {
    override def call(param: ListView[T]): ListCell[T] =
        new ListCell[T] {
            override def updateItem(value: T, empty: Boolean): Unit = {
                super.updateItem(value, empty)
                if (value != null) {
                    setText(converter(value))
                }
            }
        }
}

class OnewayStringConverter[T](converter: T => String) extends StringConverter[T] {
    override def toString(value: T): String = converter(value)

    override def fromString(string: String): T = ???
}

class PasswordStore(passwordMgr: PasswordManager) {
    def directory(id: DirectoryId): Future[Option[DirectoryInfo]] = passwordMgr.directory.get(id)

    def sheet(id: SheetId): Future[Option[(SheetInfo, Option[SheetDetail])]] = {
        implicit val ec: ExecutionContext = ExecutionContext.global
        val infoFuture = passwordMgr.sheet.get(id)
        val detailFuture = passwordMgr.sheetDetail.get(id)

        for {
            infoOpt <- infoFuture
            detailOpt <- detailFuture
        } yield {
            infoOpt match {
                case Some(info) => Some(info, detailOpt)
                case None => None
            }
        }
    }
}

class PasswordListUi(mainUi: JavaFxUI, session: Session) extends JavaFxUI.View {
    private var view: Pane = _
    private var controller: PasswordListController = _

    private val passwordManager = new PasswordManager(session)
    val passwordStore = new PasswordStore(passwordManager)

    private val directoriesList = FXCollections.observableArrayList[ListItem[DirectoryId, DirectoryInfo]]()
    private val sheetsList = FXCollections.observableArrayList[ListItem[SheetId, SheetInfo]]()

    private def setSheetMenu(editMode: Option[Boolean]): Unit = {
        controller.sheetMenuHolder.getChildren.clear()
        editMode match {
            case Some(true) =>
                controller.sheetMenuHolder.getChildren.add(controller.sheetMenuEditMode)
            case Some(false) =>
                controller.sheetMenuHolder.getChildren.add(controller.sheetMenuViewMode)
            case None => // nothing to do
        }
    }

    override def viewRoot(): Parent = {
        val loader = new FXMLLoader(getClass.getResource("/views/passwordList.fxml"))
        view = loader.load.asInstanceOf[Pane]
        controller = loader.getController.asInstanceOf[PasswordListController]

        setSheetMenu(None)

        controller.directoryComboBox.setCellFactory(new LabelListCell({
            _.name
        }))
        controller.directoryComboBox.setConverter(new OnewayStringConverter({
            _.name
        }))

        // TODO 아이콘으로 변경
        controller.sheetList.setCellFactory(new LabelListCell({
            _.name
        }))

        val lexicographicalOrder = SortScheme("사전순") { (a, b) => a.name.compareToIgnoreCase(b.name) }
        val generationOrder = SortScheme("생성순") { (a, b) => (a.id.timestamp - b.id.timestamp).toInt }
        controller.sheetListSortScheme.setItems(FXCollections.observableArrayList(
            lexicographicalOrder, generationOrder))
        controller.sheetListSortScheme.setCellFactory(new LabelListCell({
            _.name
        }))
        controller.sheetListSortScheme.setConverter(new OnewayStringConverter({
            _.name
        }))
        controller.sheetListSortScheme.getSelectionModel.select(lexicographicalOrder)

        def applySort(): Unit = {
            controller.sheetListSortScheme.getSelectionModel.getSelectedItem.sort(sheetsList)
        }

        controller.sheetListSortScheme.setOnAction { _ => applySort() }
        controller.sheetList.getSelectionModel.selectedItemProperty().addListener(new ChangeListener[ListItem[SheetId, SheetInfo]] {
            override def changed(observable: ObservableValue[_ <: ListItem[SheetId, SheetInfo]], oldValue: ListItem[SheetId, SheetInfo], newValue: ListItem[SheetId, SheetInfo]): Unit = {
                controller.sheetEditButton.setDisable(true)
                showSheetDetail(controller.directoryComboBox.getSelectionModel.getSelectedItem.value, newValue.id, newValue.value)
            }
        })

        implicit val ec: ExecutionContext = ExecutionContext.global
        passwordManager.userConfig.get() onComplete {
            case Success(configOpt) =>
                Platform.runLater { () =>
                    passwordManager.directory.directoryList() map { directories =>
                        directories foreach { directoryPair =>
                            val (directoryId, directoryInfo) = directoryPair
                            passwordManager.sheet.sheetList(directoryId) map { sheets =>
                                println(directoryInfo.name, sheets)
                                sheetsList.addAll((sheets map { sheetPair =>
                                    val (sheetId, sheetInfo) = sheetPair
                                    ListItem(sheetId)(sheetInfo, sheetInfo.name)
                                }).asJava)
                                Platform.runLater { () => applySort() }
                            }
                            directoriesList.add(ListItem(directoryId)(directoryInfo, directoryInfo.name))
                            if ((configOpt map {
                                _.defaultDirectory
                            }) contains Some(directoryId)) {
                                Platform.runLater { () =>
                                    controller.directoryComboBox.getSelectionModel.select(ListItem(directoryId)(directoryInfo, directoryInfo.name))
                                }
                            }
                        }
                    }
                    controller.directoryComboBox.setItems(directoriesList)
                    controller.sheetList.setItems(sheetsList)
                    applySort()
                }

            case Failure(reason) =>
                reason.printStackTrace()
                mainUi.showMessage(reason.getMessage, AlertType.ERROR)
        }

        controller.newSheetButton.setOnAction { _ =>
            passwordManager.sheet.createSheet(controller.directoryComboBox.getSelectionModel.getSelectedItem.id, "New Sheet", SheetType.Login) foreach { newSheetOpt =>
                val newSheet = newSheetOpt.get
                passwordManager.sheetDetail.putSheetDetail(newSheet._1, Seq()) foreach { newSheetDetailOpt =>
                    val newSheetDetail = newSheetDetailOpt.get
                    Platform.runLater { () =>
                        val listItem = ListItem(newSheet._1)(newSheet._2, newSheet._2.name)
                        controller.sheetList.getItems.add(listItem)
                        controller.sheetList.getSelectionModel.select(listItem)
                    }
                }
            }
        }

        view
    }

    private val clipboard = Clipboard.getSystemClipboard
    private val timer: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

    private def copyToClipboard(value: String, timeoutOpt: Option[Duration]): Unit = {
        val clipboardContent = new ClipboardContent()
        clipboardContent.putString(value)
        clipboard.setContent(clipboardContent)
        timeoutOpt match {
            case Some(timeout) =>
                timer.schedule(new Runnable {
                    override def run(): Unit = Platform.runLater { () =>
                        val currentClipboard = clipboard.getContent(DataFormat.PLAIN_TEXT)
                        if (currentClipboard == value) {
                            clipboard.clear()
                        }
                    }
                }, timeout.toMillis, TimeUnit.MILLISECONDS)
            case None => // nothing to do
        }
    }

    private def createSheetDetailGrid(directoryInfo: DirectoryInfo, sheetId: SheetId, sheetInfo: SheetInfo, sheetDetail: Option[SheetDetail]): Pane = {
        val gridPane = new GridPane()
        gridPane.setHgap(5)
        gridPane.setVgap(3)

        var rowNum = 0

        def addRow(name: String, rhs: Node): Unit = {
            val lhs = new Label(name)
            lhs.setTextAlignment(TextAlignment.RIGHT)
            lhs.setMinWidth(80)
            gridPane.add(lhs, 0, rowNum)
            gridPane.add(rhs, 1, rowNum)
            rowNum += 1
        }

        addRow("Directory:", new Label(directoryInfo.name))
        addRow("Type:", new Label(SheetType.mapping(sheetInfo.sheetType)))
        addRow("Sheet:", new Label(sheetInfo.name))

        sheetDetail foreach {
            _.fields.foreach { field =>
                val node: Node = field.key match {
                    case KeyType.Username =>
                        val copyButton = new Button("Copy")
                        copyButton.setOnAction { _ => copyToClipboard(field.value, None) }
                        val textField = new TextField(field.value)
                        textField.setEditable(false)
                        val hbox = new HBox(
                            copyButton,
                            textField
                        )
                        hbox
                    case KeyType.Password =>
                        val copyButton = new Button("Copy")
                        copyButton.setOnAction { _ => copyToClipboard(field.value, Some(1.minute)) }
                        val hbox = new HBox(
                            copyButton,
                            new Label("****")
                        )
                        hbox
                    case _ =>
                        new Label(field.value)
                }
                addRow(KeyType.mapping(field.key), node)
            }
        }

        gridPane
    }

    def showSheetDetail(directoryInfo: DirectoryInfo, sheetId: SheetId, sheetInfo: SheetInfo): Unit = {
        implicit val ec: ExecutionContext = ExecutionContext.global
        passwordStore.sheet(sheetId) foreach { sheetDetail: Option[(SheetInfo, Option[SheetDetail])] =>
            Platform.runLater { () =>
                val selected = controller.sheetList.getSelectionModel.getSelectedItems
                if (selected.size() == 1 && selected.get(0).id == sheetId) {
                    setSheetMenu(Some(false))
                    controller.sheetContentHolder.setContent(createSheetDetailGrid(directoryInfo, sheetId, sheetInfo, sheetDetail flatMap {
                        _._2
                    }))
                    controller.sheetEditButton.setDisable(false)
                    controller.sheetEditButton.setOnAction { _ =>
                        editSheetDetail(directoryInfo, sheetId, sheetInfo, sheetDetail)
                    }
                }
            }
        }
    }

    private val monospaced = Font.font("monospaced")

    def createSheetEditGrid(directoryInfo: DirectoryInfo, sheetId: SheetId, sheetInfo: SheetInfo, sheetDetail: Option[SheetDetail]): Pane = {
        class SheetEdit {
            val borderPane = new BorderPane()
            val jsonPreview = new TextArea()
            val editGrid = new GridPane()

            private var rowNum = 0

            def updateJsonPreview(sheetId: SheetId, sheetInfo: SheetInfo, sheetDetail: Option[SheetDetail]): Unit = {
                val previewText =
                    s"""${passwordManager.sheetPath(sheetId).string}
                       |${pretty(render(passwordManager.sheet.infoJsonOf(sheetInfo)))}
                       |${sheetDetail map { d => pretty(render(passwordManager.sheetDetail.jsonOf(d))) }}
                 """.stripMargin
                jsonPreview.setText(previewText)
            }

            def key(name: String): Label = {
                val lhs = new Label(name)
                lhs.setTextAlignment(TextAlignment.RIGHT)
                lhs.setMinWidth(80)
                lhs
            }

            def addRow(key: Node, rhs: Node, deletable: Option[CheckBox]): Unit = {
                editGrid.add(key, 0, rowNum)
                editGrid.add(rhs, 1, rowNum)
                if (deletable.isDefined) {
                    editGrid.add(deletable.get, 2, rowNum)
                }
                rowNum += 1
            }

            val sheetName = new TextField(sheetInfo.name)

            def init(): Unit = {
                val vbox = new VBox()

                borderPane.setCenter(vbox)
                borderPane.setBottom(jsonPreview)

                jsonPreview.setFont(monospaced)
                jsonPreview.setEditable(false)

                editGrid.setHgap(5)
                editGrid.setVgap(3)

                controller.sheetDeleteButton.setOnAction { _ => ??? }
                controller.sheetEditSaveButton.setOnAction { _ =>
                    val (sheetInfo, sheetDetail) = createSheetInfo()
                    implicit val ec = ExecutionContext.global
                    passwordManager.sheet.updateSheet(sheetId, sheetInfo) onComplete { newSheetInfo =>
                        passwordManager.sheetDetail.putSheetDetail(sheetId, sheetDetail) onComplete { _ =>
                            showSheetDetail(directoryInfo, sheetId, sheetInfo)
                            Platform.runLater { () =>
                                val idx = controller.sheetList.getItems.indexOf(ListItem(sheetId)(sheetInfo, sheetInfo.name))
                                val newSheetInfoGet = newSheetInfo.get.get
                                controller.sheetList.getItems.set(idx, ListItem(sheetId)(newSheetInfoGet, newSheetInfoGet.name))
                            }
                        }
                    }
                }
                controller.sheetEditCancelButton.setOnAction { _ => showSheetDetail(directoryInfo, sheetId, sheetInfo) }

                addRow(key("Directory:"), new Label(directoryInfo.name), None)
                addRow(key("Type:"), new Label(SheetType.mapping(sheetInfo.sheetType)), None)

                sheetName.textProperty().addListener(changeListener[String])
                addRow(key("Sheet:"), sheetName, None)

                updateJsonPreview(sheetId, sheetInfo, sheetDetail)
                vbox.getChildren.addAll(editGrid, newFieldBtn)

                fields = sheetDetail match {
                    case Some(detail) =>
                        detail.fields map { field => addField(field.key, field.value) }
                    case None => List()
                }

                newFieldBtn.setOnAction { _ =>
                    fields :+= addField(KeyType.Unknown, "")
                }
            }

            def changeListener[T]: ChangeListener[T] = (_: ObservableValue[_ <: T], _: T, _: T) => updateJsonPreviewToLatest()

            def addField(key: KeyType.Value, value: String): (ComboBox[Password.KeyType.Value], TextField, CheckBox) = {
                val comboBox = new ComboBox[KeyType.Value]()
                comboBox.getItems.addAll(KeyType.mapping.keys.toList: _*)
                comboBox.getSelectionModel.select(key)

                val textField = new TextField(value)

                val deleteButton = new CheckBox("-")
                addRow(comboBox, textField, Some(deleteButton))

                comboBox.valueProperty().addListener(changeListener[Password.KeyType.Value])
                textField.textProperty().addListener(changeListener[String])
                deleteButton.selectedProperty().addListener(changeListener[Any])

                (comboBox, textField, deleteButton)
            }

            var fields: Seq[(ComboBox[Password.KeyType.Value], TextField, CheckBox)] = Seq()

            val newFieldBtn = new Button("+")

            def createSheetInfo(): (SheetInfo, SheetDetail) = {
                val newSheetInfo = SheetInfo(sheetName.textProperty().get(), sheetInfo.sheetType)
                val newSheetDetail = SheetDetail(fields filterNot {
                    _._3.isSelected
                } map { field =>
                    Password.Field(field._1.valueProperty().get(), field._2.textProperty().get())
                })
                (newSheetInfo, newSheetDetail)
            }

            def updateJsonPreviewToLatest(): Unit = {
                val (sheetInfo, sheetDetail) = createSheetInfo()
                updateJsonPreview(sheetId, sheetInfo, Some(sheetDetail))
            }
        }

        val edit = new SheetEdit()

        edit.init()
        edit.borderPane
    }

    def editSheetDetail(directoryInfo: DirectoryInfo, sheetId: SheetId, sheetInfo: SheetInfo, sheetDetail: Option[(SheetInfo, Option[SheetDetail])]): Unit = {
        implicit val ec: ExecutionContext = ExecutionContext.global
        setSheetMenu(Some(true))
        controller.sheetContentHolder.setContent(createSheetEditGrid(directoryInfo, sheetId, sheetInfo, sheetDetail flatMap {
            _._2
        }))
    }
}
