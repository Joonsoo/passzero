package com.giyeok.passzero.ui.swt

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Random
import scala.util.Success
import com.giyeok.passzero.Password
import com.giyeok.passzero.Password.SheetDetail
import com.giyeok.passzero.Password.SheetType
import com.giyeok.passzero.PasswordManager
import com.giyeok.passzero.Session
import com.giyeok.passzero.ui.Config
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
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
            passwordMgr.directory.createDirectory(s"울랄라 ${Math.abs(Random.nextInt())}")
        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    newSheetBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            directoryList.selectedItem foreach { pair =>
                val directory = pair._1.data
                passwordMgr.sheet.createSheet(directory, s"amazon.com ${Math.abs(Random.nextInt())}", SheetType.Login)
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

    private val directoryList = new SortedList[TextSortedListItem[Password.Directory]](getDisplay, this, SWT.BORDER)
    directoryList.setLayoutData(fillAll())
    directoryList.addSelectListener({ selectedOpt =>
        setSelectedDirectory(selectedOpt map { _._1.data })
    })

    private val sheetList = new SortedList[TextSortedListItem[Password.Sheet]](getDisplay, this, SWT.BORDER)
    sheetList.setLayoutData(fillAll())
    sheetList.addSelectListener({ selectedOpt =>
        setSelectedSheet(selectedOpt map { _._1.data })
    })

    private val sheetView = new SheetContentView(this, SWT.BORDER)
    sheetView.setLayoutData(fillAll())

    private def start(): Unit = {
        session.ensureInitialized() onComplete {
            case Success(_) =>
                getDisplay.syncExec(() =>
                    directoryList.setSource(passwordMgr.directory.directoryList() map { directories =>
                        directories map { directory => TextSortedListItem(directory, directory.name) }
                    }))
            case Failure(reason) =>
                reason.printStackTrace()
                getDisplay.syncExec(() => showMessage(reason.getMessage))
        }
    }
    start()

    private def setSelectedDirectory(directoryOpt: Option[Password.Directory]): Unit = {
        directoryOpt match {
            case Some(directory) =>
                sheetList.setSource(passwordMgr.sheet.sheetList(directory) map { sheets =>
                    sheets map { sheet => TextSortedListItem(sheet, sheet.name) }
                })
                sheetView.clearAll()
            case None =>
                sheetView.clearAll()
        }
    }

    private def setSelectedSheet(sheetOpt: Option[Password.Sheet]): Unit = {
        sheetOpt match {
            case Some(sheet) =>
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
            case None =>
                sheetView.clearAll()
        }
    }
}

class SheetContentView(parent: Composite, style: Int) extends Composite(parent, style) {
    def clearAll(): Unit = {
    }

    def setDetail(detail: SheetDetail): Unit = {
    }

    def setError(): Unit = {
    }
}
