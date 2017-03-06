package com.giyeok.passzero.ui.swt

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import com.giyeok.passzero.Password
import com.giyeok.passzero.Password.Directory
import com.giyeok.passzero.Password.Sheet
import com.giyeok.passzero.PasswordManager
import com.giyeok.passzero.Session
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.ui.Config
import com.giyeok.passzero.utils.ByteArrayUtil._
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Table

class PasswordListUI(val shell: Shell, parent: MainUI, style: Int, session: Session, config: Config)
        extends Composite(parent, style) with WidgetUtil with ClipboardUtil with GridLayoutUtil with MessageBoxUtil {
    implicit private val ec = ExecutionContext.global

    shell.setText(config.stringRegistry.get("PasswordListUI"))
    setLayout(new GridLayout(3, false))

    private val saveBtn = button("Save")
    private val loadBtn = button("Load")
    private val copyBtn = button("Copy")

    private val emergencyKitBtn = button("Emergency Kit", rightest(3))

    saveBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            session.put(Path("hello"), "helloworld".toBytes)
        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    loadBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            val list = session.list(Path(""))
            list foreach println

            session.getAsString(Path("hello")) foreach { s =>
                showMessage(s.get.content)
            }
        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    copyBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            session.getAsString(Path("hello")) foreach { s =>
                putTextToClipboard(s.get.content, Some(30.seconds))
            }
        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    emergencyKitBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            parent.pushEmergencyKit(session.localInfo)
        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    private val passwordMgr = new PasswordManager(session)

    private val directoryList = new SortedList[Password.Directory](getDisplay, this, SWT.NONE) {
        override def >(a: Directory, b: Directory): Boolean = a.name > b.name
        override def repr(item: Directory): String = item.name
        override def selected(item: Directory, index: Int): Unit = setSelectedDirectory(item)
    }

    private val sheetList = new SortedList[Password.Sheet](getDisplay, this, SWT.NONE) {
        override def >(a: Sheet, b: Sheet): Boolean = a.name > b.name
        override def repr(item: Sheet): String = item.name
        override def selected(item: Sheet, index: Int): Unit = setSelectedSheet(item)
    }

    private val sheetView = new Table(this, SWT.NONE)

    private def start(): Unit = {
        Future {
            session.ensureInitialized()
        } onComplete {
            case Success(_) =>
                directoryList.setSource(passwordMgr.directoryList())
            case Failure(reason) =>
                reason.printStackTrace()
                getDisplay.syncExec(() => showMessage(reason.getMessage))
        }
    }
    start()

    private def setSelectedDirectory(directory: Password.Directory): Unit = {
        sheetList.setSource(passwordMgr.sheetList(directory))
        sheetView.clearAll()
    }

    private def setSelectedSheet(sheet: Password.Sheet): Unit = {
        Future {
            passwordMgr.content(sheet)
        } foreach { fields =>
            getDisplay.syncExec(() => {
                // sheetView.setFields(fields)
            })
        }
    }
}

abstract class SortedList[T](display: Display, parent: Composite, style: Int) {
    val listWidget = new widgets.List(parent, style)
    private var items = Seq[T]()

    listWidget.addSelectionListener(new SelectionListener {
        override def widgetSelected(e: SelectionEvent): Unit = {
            val index = listWidget.getSelectionIndex
            if (0 <= index && index < items.length) {
                selected(items(index), index)
            }
        }

        override def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    def clear(): Unit = {
        listWidget.removeAll()
        items = Seq()

    }

    def setSource(stream: Stream[T]): Unit = {
        clear()
        stream.foldLeft(Seq[T]()) { (list, item) =>
            val index = list.zipWithIndex find { p => >(p._1, item) } map { _._2 } getOrElse list.length
            val (init, tail) = list.splitAt(index)
            val newList: Seq[T] = init ++ (item +: tail)
            display.syncExec(() => { listWidget.add(repr(item), index) })
            items = newList
            newList
        }
    }

    def >(a: T, b: T): Boolean // = a > b
    def repr(item: T): String // = item.name
    def selected(item: T, index: Int): Unit
}
