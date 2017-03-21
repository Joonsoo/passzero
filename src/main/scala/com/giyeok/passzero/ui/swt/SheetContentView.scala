package com.giyeok.passzero.ui.swt

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import com.giyeok.passzero.Password.DirectoryId
import com.giyeok.passzero.Password.DirectoryInfo
import com.giyeok.passzero.Password.Field
import com.giyeok.passzero.Password.KeyType
import com.giyeok.passzero.Password.SheetDetail
import com.giyeok.passzero.Password.SheetId
import com.giyeok.passzero.Password.SheetInfo
import com.giyeok.passzero.Password.SheetType
import com.giyeok.passzero.ui.Config
import com.giyeok.passzero.ui.swt.GridLayoutUtil._
import com.giyeok.passzero.ui.swt.WidgetUtil._
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.CCombo
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.custom.StackLayout
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Combo
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Text

object SheetContentView {
    val baseBackgroundColor = new Color(null, 255, 255, 255)
    val editableBackgroundColor = new Color(null, 220, 220, 220)
}

class SheetContentView(config: Config, parent: Composite, style: Int, passwordUi: PasswordListUI, passwordStore: PasswordStore)
        extends Composite(parent, style) with FormLayoutUtil {
    setLayout(new FormLayout())

    private val content = new ScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL)
    content.setExpandHorizontal(true)
    content.setExpandVertical(true)

    private var currentContent: Option[EditableContent] = None

    class CommandButtons() extends Composite(SheetContentView.this, SWT.NONE) {
        val stackLayout = new StackLayout
        stackLayout.marginHeight = 0
        stackLayout.marginWidth = 0
        setLayout(stackLayout)

        def updateState(): Unit = {
            if (currentContent.isEmpty) {
                stackLayout.topControl = emptyCommands
            } else {
                if (editing) {
                    stackLayout.topControl = editingCommands
                } else {
                    stackLayout.topControl = notEditingCommands
                }
            }
            requestLayout()
            println(computeSize(SWT.DEFAULT, SWT.DEFAULT))
            val preferredSize = stackLayout.topControl.computeSize(SWT.DEFAULT, SWT.DEFAULT)
            setSize(preferredSize)
            println(preferredSize)
            this.setLayoutData(formData(left = att(0, 0), right = att(100, 0), top = att(100, -preferredSize.y), bottom = att(100, 0)))
            SheetContentView.this.requestLayout()
        }

        val emptyCommands = new Composite(this, SWT.NONE)

        private val notEditingCommands = new Composite(this, SWT.NONE)
        notEditingCommands.setLayout(gridLayoutNoMargin(1, equalWidths = false))
        private val editButton = button(notEditingCommands, config.stringRegistry.get("Edit"), SWT.NONE, rightest() and { _.grabExcessHorizontalSpace = true })

        private val editingCommands = new Composite(this, SWT.NONE)
        editingCommands.setBackground(SheetContentView.editableBackgroundColor)
        editingCommands.setLayout(gridLayoutNoMargin(3, equalWidths = false))
        private val deleteSheetButton = button(editingCommands, config.stringRegistry.get("Delete Sheet"), SWT.NONE, rightest() and { _.grabExcessHorizontalSpace = true })
        private val saveButton = button(editingCommands, config.stringRegistry.get("Save"), SWT.NONE, rightest())
        private val cancelButton = button(editingCommands, config.stringRegistry.get("Cancel"), SWT.NONE, rightest())

        emptyCommands.setBackground(SheetContentView.baseBackgroundColor)
        notEditingCommands.setBackground(SheetContentView.baseBackgroundColor)
        editingCommands.setBackground(SheetContentView.baseBackgroundColor)

        editButton.addSelectionListener(new SelectionListener {
            override def widgetDefaultSelected(e: SelectionEvent): Unit = {}
            override def widgetSelected(e: SelectionEvent): Unit = {
                currentContent foreach { _.editMode() }
                setEditing(true)
            }
        })
        deleteSheetButton.addSelectionListener(new SelectionListener {
            override def widgetDefaultSelected(e: SelectionEvent): Unit = {}
            override def widgetSelected(e: SelectionEvent): Unit = {
                // TODO 확인 다이얼로그
                selectedSheetId foreach { sheetId => passwordUi.removeSheet(sheetId) }
            }
        })
        saveButton.addSelectionListener(new SelectionListener {
            override def widgetDefaultSelected(e: SelectionEvent): Unit = {}
            override def widgetSelected(e: SelectionEvent): Unit = {
                currentContent foreach { _.commit() }
                setEditing(false)
            }
        })
        cancelButton.addSelectionListener(new SelectionListener {
            override def widgetDefaultSelected(e: SelectionEvent): Unit = {}
            override def widgetSelected(e: SelectionEvent): Unit = {
                currentContent foreach { _.cancel() }
                setEditing(false)
            }
        })
    }

    private val commands = new CommandButtons()
    content.setLayoutData(formData(att(0, 0), att(0, 0), right = att(100, 0), bottom = att(commands)))
    commands.setLayoutData(formData(left = att(0, 0), right = att(100, 0), bottom = att(100, 0)))

    private var editing: Boolean = false
    private def setEditing(editing: Boolean): Unit = {
        this.editing = editing
        this.commands.updateState()
    }

    trait EditableContent {
        def editMode(): Unit
        def commit(): Unit
        def cancel(): Unit
    }
    def replaceContent(newContentFunc: () => Control): Unit = {
        if (!this.isDisposed && !content.isDisposed) {
            val oldChildren = content.getChildren
            val newContent = newContentFunc()
            currentContent = newContent match {
                case ec: EditableContent => Some(ec)
                case _ => None
            }
            setEditing(false)
            content.setContent(newContent)
            content.setMinSize(newContent.computeSize(SWT.DEFAULT, SWT.DEFAULT))
            oldChildren foreach { _.dispose() }
            requestLayout()
        }
    }

    private class EmptyContent() extends Composite(content, SWT.NONE) {
        setLayout(new FillLayout)
        setBackground(SheetContentView.baseBackgroundColor)
    }

    private class DirectoryContent(id: DirectoryId, info: DirectoryInfo) extends Composite(content, SWT.NONE) with EditableContent {
        setLayout(gridLayoutNoMargin(2, equalWidths = false))
        setBackground(SheetContentView.baseBackgroundColor)

        label(this, config.stringRegistry.get("Directory:"), leftLabel())
        private val directoryName = text(this, info.name, SWT.READ_ONLY, horizontalFill())

        def editMode(): Unit = {
            directoryName.setEditable(true)
            directoryName.setBackground(SheetContentView.editableBackgroundColor)
        }
        def commit(): Unit = {
            this.setEnabled(false)
            passwordUi.updateDirectory(id, directoryName.getText)
            // 업데이트가 완료되면 이 Content는 다른 내용으로 치환될 것
        }
        def cancel(): Unit = {
            directoryName.setEditable(false)
            directoryName.setBackground(SheetContentView.baseBackgroundColor)
            directoryName.setText(info.name)
        }
    }

    def emptyContent(): Unit = {
        replaceContent(() => new EmptyContent())
    }

    private class SheetDetailContent(parent: Composite, id: SheetId, fields: Seq[Field])
            extends Composite(parent, SWT.NONE) with EditableContent with ClipboardUtil {
        setLayout(gridLayoutNoMargin(3, equalWidths = false))
        setBackground(SheetContentView.baseBackgroundColor)

        private class FieldView(key: KeyType.Value, value: String, startAsEditMode: Boolean = false) {
            private var controls: Seq[Control] = Seq()
            private var editControls: Option[(CCombo, Text, Button)] = None

            def head: Control = controls.head
            def last: Control = controls.last
            def dispose(): Unit = {
                controls foreach { _.dispose() }
            }

            def replaceControls(newControls: Seq[Control]): Unit = {
                controls foreach { _.dispose() }
                controls = newControls
            }
            def viewMode(): Unit = {
                editControls = None
                key match {
                    case KeyType.Password =>
                        val typeLabel = label(SheetDetailContent.this, config.stringRegistry.get(KeyType.mapping(key)), leftLabel())
                        val passwordText = text(SheetDetailContent.this, "****", SWT.READ_ONLY, horizontalFill())
                        val copyButton = button(SheetDetailContent.this, config.stringRegistry.get("Copy"), SWT.FLAT, rightest())
                        passwordText.setBackground(SheetContentView.baseBackgroundColor)
                        copyButton.addSelectionListener(new SelectionListener {
                            override def widgetDefaultSelected(e: SelectionEvent): Unit = {}
                            override def widgetSelected(e: SelectionEvent): Unit = {
                                putTextToClipboard(value, Some(1.minute))
                            }
                        })
                        replaceControls(Seq(typeLabel, passwordText, copyButton))
                    case _ =>
                        val typeLabel = label(SheetDetailContent.this, config.stringRegistry.get(KeyType.mapping(key)), leftLabel())
                        val valueText = text(SheetDetailContent.this, value, SWT.READ_ONLY, horizontalFill(2))
                        valueText.setBackground(SheetContentView.baseBackgroundColor)
                        replaceControls(Seq(typeLabel, valueText))
                }
            }

            // TODO password 생성기 추가
            def editMode(): Unit = {
                val typeSelector = ccombo(SheetDetailContent.this, KeyType.mapping.toSeq map { _._2 }, SWT.FLAT | SWT.READ_ONLY, leftLabel())
                val valueText = text(SheetDetailContent.this, value, horizontalFill())
                val removeButton = button(SheetDetailContent.this, "-", SWT.FLAT | SWT.CHECK, rightest())
                typeSelector.select((KeyType.mapping.toSeq.zipWithIndex find { _._1._1 == key }).get._2)
                valueText.setBackground(SheetContentView.editableBackgroundColor)
                replaceControls(Seq(typeSelector, valueText, removeButton))
                editControls = Some(typeSelector, valueText, removeButton)
            }

            def newField: Option[Field] = {
                // editMode에서만 동작
                editControls flatMap { c =>
                    val (typeSelector, valueText, removeButton) = c
                    if (removeButton.getSelection) None
                    else Some(Field(KeyType.mapping.toSeq(typeSelector.getSelectionIndex)._1, valueText.getText))
                }
            }

            if (startAsEditMode) editMode() else viewMode()
        }

        private val fieldsControls: Seq[FieldView] = fields map { f => new FieldView(f.key, f.value) }

        private val newField = button(this, "+", leftest(3))
        newField.setVisible(false)

        private var newFieldsControls: Seq[FieldView] = Seq()

        newField.addSelectionListener(new SelectionListener {
            override def widgetDefaultSelected(e: SelectionEvent): Unit = {}
            override def widgetSelected(e: SelectionEvent): Unit = {
                val fieldView = new FieldView(KeyType.Note, "", true)
                newFieldsControls :+= fieldView
                newField.moveBelow(fieldView.last)
                SheetDetailContent.this.requestLayout()
            }
        })

        def editMode(): Unit = {
            newField.setVisible(true)
            fieldsControls foreach { _.editMode() }
            newField.moveBelow(getChildren.last)
            requestLayout()
        }

        def commit(): Unit = {
            val oldFields: Seq[Field] = fieldsControls flatMap { _.newField }
            val newFields: Seq[Field] = newFieldsControls flatMap { _.newField }
            passwordUi.updateSheetDetail(id, oldFields ++ newFields)
        }

        def cancel(): Unit = {
            newField.setVisible(false)
            fieldsControls foreach { _.viewMode() }
            newFieldsControls foreach { _.dispose() }
            newFieldsControls = Seq()
            newField.moveBelow(getChildren.last)
            requestLayout()
        }
    }

    private class SheetContent(id: SheetId, directoryInfo: DirectoryInfo, sheetInfo: SheetInfo, detail: Option[SheetDetail])
            extends Composite(content, SWT.NONE) with EditableContent {
        setLayout(gridLayoutNoMargin(2, equalWidths = false))
        setBackground(SheetContentView.baseBackgroundColor)

        label(this, config.stringRegistry.get("Directory:"), leftLabel())
        private val directoryName = text(this, directoryInfo.name, SWT.READ_ONLY, horizontalFill())

        label(this, config.stringRegistry.get("Type:"), leftLabel())
        private val sheetType = label(this, config.stringRegistry.get(SheetType.mapping(sheetInfo.sheetType)), horizontalFill())

        label(this, config.stringRegistry.get("Sheet:"), leftLabel())
        private val sheetName = text(this, sheetInfo.name, SWT.READ_ONLY, horizontalFill())

        private val fieldsComposite = new SheetDetailContent(this, id, detail map { _.fields } getOrElse Seq())
        fieldsComposite.setLayoutData(fillAll(2))

        def editMode(): Unit = {
            sheetName.setFocus()
            directoryName.setEditable(true)
            directoryName.setBackground(SheetContentView.editableBackgroundColor)
            sheetName.setEditable(true)
            sheetName.setBackground(SheetContentView.editableBackgroundColor)
            fieldsComposite.editMode()
            requestLayout()
        }
        def commit(): Unit = {
            fieldsComposite.commit()
            this.setEnabled(false)
            passwordUi.updateDirectory(id.directoryId, directoryName.getText)
            // TODO sheet Type 업데이트
            passwordUi.updateSheet(id, sheetName.getText, sheetInfo.sheetType)
            // passwordUi.updateSheetDetail(id, fields)
            // 업데이트가 완료되면 이 Content는 다른 내용으로 치환될 것
        }
        def cancel(): Unit = {
            fieldsComposite.cancel()
            directoryName.setEditable(false)
            directoryName.setBackground(SheetContentView.baseBackgroundColor)
            sheetName.setEditable(false)
            sheetName.setBackground(SheetContentView.baseBackgroundColor)
            // directoryName.setText(id.directory.name)
            sheetName.setText(sheetInfo.name)
            requestLayout()
        }
    }

    def setDirectory(id: DirectoryId, info: DirectoryInfo): Unit = {
        replaceContent(() => new DirectoryContent(id, info))
    }

    private var selectedSheetId: Option[SheetId] = None
    def setSheet(id: Option[SheetId]): Unit = {
        selectedSheetId = id
        refreshSheet()
    }

    // TODO passwordStore 받아서 처리하도록 수정
    def refreshSheet(): Unit = {
        // TODO progress 표시
        implicit val ec = ExecutionContext.global
        selectedSheetId match {
            case Some(sheetId) =>
                val directoryFuture = passwordStore.directory(sheetId.directoryId)
                val sheetInfoFuture = passwordStore.sheet(sheetId)
                for {
                    directoryInfoOpt <- directoryFuture
                    sheetInfoOpt <- sheetInfoFuture
                } yield {
                    (directoryInfoOpt, sheetInfoOpt) match {
                        case (Some(directoryInfo), Some((sheetInfo, detail))) =>
                            getDisplay.syncExec(() => replaceContent(() => new SheetContent(sheetId, directoryInfo, sheetInfo, detail)))
                        case _ => // TODO error
                    }
                }
            case None =>
                replaceContent(() => new EmptyContent())
        }
    }
}
