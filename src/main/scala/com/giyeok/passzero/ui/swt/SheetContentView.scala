package com.giyeok.passzero.ui.swt

import com.giyeok.passzero.Password.Directory
import com.giyeok.passzero.Password.Field
import com.giyeok.passzero.Password.Sheet
import com.giyeok.passzero.Password.SheetDetail
import com.giyeok.passzero.Password.SheetType
import com.giyeok.passzero.PasswordManager
import com.giyeok.passzero.ui.Config
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.custom.StackLayout
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control

object SheetContentView {
    val baseBackgroundColor = new Color(null, 255, 255, 255)
}

class SheetContentView(config: Config, parent: Composite, style: Int, passwordUi: PasswordListUI)
        extends Composite(parent, style) with WidgetUtil with FormLayoutUtil {
    setLayout(new FormLayout())

    private val content = new ScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL)
    content.setExpandHorizontal(true)
    content.setExpandVertical(true)

    private var currentContent: Option[EditableContent] = None

    class CommandButtons() extends Composite(SheetContentView.this, SWT.NONE) with GridLayoutUtil {
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

        val notEditingCommands = new Composite(this, SWT.NONE)
        notEditingCommands.setLayout(gridLayoutNoMargin(1, equalWidths = false))
        val editButton = new Button(notEditingCommands, SWT.NONE)
        editButton.setText("Edit")
        editButton.setLayoutData(rightest() and { _.grabExcessHorizontalSpace = true })

        val editingCommands = new Composite(this, SWT.NONE)
        editingCommands.setLayout(gridLayoutNoMargin(2, equalWidths = false))
        val saveButton = new Button(editingCommands, SWT.NONE)
        saveButton.setText("Save")
        saveButton.setLayoutData(rightest() and { _.grabExcessHorizontalSpace = true })
        val cancelButton = new Button(editingCommands, SWT.NONE)
        cancelButton.setText("Cancel")
        cancelButton.setLayoutData(rightest())

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

    private class EmptyContent() extends Composite(content, SWT.NONE) with WidgetUtil with GridLayoutUtil {
        setLayout(new FillLayout)
        setBackground(SheetContentView.baseBackgroundColor)
    }

    private class DirectoryContent(directory: Directory) extends Composite(content, SWT.NONE) with EditableContent with WidgetUtil with GridLayoutUtil {
        setLayout(gridLayoutNoMargin(2, equalWidths = false))
        setBackground(SheetContentView.baseBackgroundColor)

        label(config.stringRegistry.get("Directory:"), leftLabel())
        private val directoryName = text(directory.name, SWT.READ_ONLY, horizontalFill())

        def editMode(): Unit = {
            directoryName.setEditable(true)
        }
        def commit(): Unit = {
            this.setEnabled(false)
            passwordUi.updateDirectory(directory, directoryName.getText)
            // 업데이트가 완료되면 이 Content는 다른 내용으로 치환될 것
        }
        def cancel(): Unit = {
            directoryName.setEditable(false)
            directoryName.setText(directory.name)
        }
    }

    def emptyContent(): Unit = {
        replaceContent(() => new EmptyContent())
    }

    private class SheetContent(sheet: Sheet, fields: Seq[Field]) extends Composite(content, SWT.NONE) with EditableContent with WidgetUtil with GridLayoutUtil {
        setLayout(gridLayoutNoMargin(2, equalWidths = false))
        setBackground(SheetContentView.baseBackgroundColor)

        label(config.stringRegistry.get("Directory:"), leftLabel())
        private val directoryName = text(sheet.directory.name, SWT.READ_ONLY, horizontalFill())

        label(config.stringRegistry.get("Type:"), leftLabel())
        private val sheetType = label(config.stringRegistry.get(SheetType.mapping(sheet.sheetType)))

        label(config.stringRegistry.get("Sheet:"), leftLabel())
        private val sheetName = text(sheet.name, SWT.READ_ONLY, horizontalFill())

        def editMode(): Unit = {
            directoryName.setEditable(true)
            sheetName.setEditable(true)
        }
        def commit(): Unit = {
            this.setEnabled(false)
            passwordUi.updateDirectory(sheet.directory, directoryName.getText)
            // TODO sheet Type 업데이트
            passwordUi.updateSheet(sheet, sheetName.getText, sheet.sheetType)
            passwordUi.updateSheetDetail(sheet, fields)
            // 업데이트가 완료되면 이 Content는 다른 내용으로 치환될 것
        }
        def cancel(): Unit = {
            directoryName.setEditable(false)
            sheetName.setEditable(false)
            directoryName.setText(sheet.directory.name)
            sheetName.setText(sheet.name)
        }
    }

    def setDirectory(directory: Directory): Unit = {
        replaceContent(() => new DirectoryContent(directory))
    }

    def setSheet(sheet: Sheet): Unit = {
        replaceContent(() => new SheetContent(sheet, Seq()))
    }

    def setDetailedSheet(sheet: Sheet, detail: SheetDetail): Unit = {
        replaceContent(() => new SheetContent(sheet, Seq()))
    }
}
