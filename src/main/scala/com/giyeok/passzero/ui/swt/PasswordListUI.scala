package com.giyeok.passzero.ui.swt

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import com.giyeok.passzero.Password
import com.giyeok.passzero.Password.Directory
import com.giyeok.passzero.Password.Sheet
import com.giyeok.passzero.Password.SheetDetail
import com.giyeok.passzero.Password.SheetType
import com.giyeok.passzero.PasswordManager
import com.giyeok.passzero.Session
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.ui.Config
import com.giyeok.passzero.utils.FutureStream
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell

class PasswordListUI(val shell: Shell, parent: MainUI, style: Int, session: Session, config: Config)
        extends Composite(parent, style) with WidgetUtil with ClipboardUtil with GridLayoutUtil with MessageBoxUtil {
    implicit private val ec = ExecutionContext.global

    private val passwordMgr = new PasswordManager(session)

    shell.setText(config.stringRegistry.get("PasswordListUI"))
    setLayout(new GridLayout(3, true))

    private val refreshAllBtn = button("Refresh All")
    private val newDirectoryBtn = button("New Directory")
    private val newSheetBtn = button("New Sheet")
    private val copyBtn = button("Copy")

    private val emergencyKitBtn = button("Emergency Kit", rightest(3))

    refreshAllBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            start()
        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    newDirectoryBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            passwordMgr.directory.createDirectory("울랄라")
        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    newSheetBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            directoryList.selectedItem foreach { pair =>
                val directory = pair._1
                passwordMgr.sheet.createSheet(directory, "amazon.com", SheetType.Login)
            }
        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    copyBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            putTextToClipboard("Hello~", Some(30.seconds))
        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    emergencyKitBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            parent.pushEmergencyKit(session.localInfo)
        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    private val directoryList = new SortedList[Password.Directory](getDisplay, this, SWT.BORDER) {
        override def >(a: Directory, b: Directory): Boolean = a.name > b.name
        override def repr(item: Directory): String = item.name
        override def selected(item: Directory, index: Int): Unit = setSelectedDirectory(item)
    }
    directoryList.listWidget.setLayoutData(fillAll())

    private val sheetList = new SortedList[Password.Sheet](getDisplay, this, SWT.BORDER) {
        override def >(a: Sheet, b: Sheet): Boolean = a.name > b.name
        override def repr(item: Sheet): String = item.name
        override def selected(item: Sheet, index: Int): Unit = setSelectedSheet(item)
    }
    sheetList.listWidget.setLayoutData(fillAll())

    private val sheetView = new SheetContentView(this, SWT.BORDER)
    sheetView.setLayoutData(fillAll())

    private def start(): Unit = {
        session.ensureInitialized() onComplete {
            case Success(_) =>
                getDisplay.syncExec(() =>
                    directoryList.setSource(passwordMgr.directory.directoryList()))
            case Failure(reason) =>
                reason.printStackTrace()
                getDisplay.syncExec(() => showMessage(reason.getMessage))
        }
    }
    start()

    private def setSelectedDirectory(directory: Password.Directory): Unit = {
        sheetList.setSource(passwordMgr.sheet.sheetList(directory))
        sheetView.clearAll()
    }

    private def setSelectedSheet(sheet: Password.Sheet): Unit = {
        passwordMgr.sheetDetail.sheetDetail(sheet) foreach { detailOpt =>
            getDisplay.syncExec(() => {
                detailOpt match {
                    case Some(detail) =>
                        sheetView.setDetail(detail)
                    case None =>
                        sheetView.setError()
                }
            })
        }
    }
}

abstract class SortedList[T](display: Display, parent: Composite, style: Int) {
    val listWidget = new widgets.List(parent, style)
    private var items = Seq[T]()
    private var _selectedItem = Option.empty[(T, Int)]

    def selectedItem: Option[(T, Int)] = _selectedItem

    listWidget.addSelectionListener(new SelectionListener {
        override def widgetSelected(e: SelectionEvent): Unit = {
            val index = listWidget.getSelectionIndex
            if (0 <= index && index < items.length) {
                _selectedItem = Some(items(index), index)
                selected(items(index), index)
            }
        }

        override def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    def clear(): Unit = {
        listWidget.removeAll()
        items = Seq()
    }

    def setSource(stream: FutureStream[Seq[T]]): Unit = {
        clear()
        stream foreach { page =>
            page foreach { item =>
                val index = items.zipWithIndex find { p => >(p._1, item) } map { _._2 } getOrElse items.length
                val (init, tail) = items.splitAt(index)
                val newList: Seq[T] = init ++ (item +: tail)
                display.syncExec(() => { listWidget.add(repr(item), index) })
                items = newList
                newList
            }
        }
    }

    def >(a: T, b: T): Boolean // = a > b
    def repr(item: T): String // = item.name
    def selected(item: T, index: Int): Unit
}

class SheetContentView(parent: Composite, style: Int) extends Composite(parent, style) {
    def clearAll(): Unit = {
    }

    def setDetail(detail: SheetDetail): Unit = {
    }

    def setError(): Unit = {
    }
}
