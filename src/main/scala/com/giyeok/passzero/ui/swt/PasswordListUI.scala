package com.giyeok.passzero.ui.swt

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Random
import scala.util.Success
import com.giyeok.passzero.Password
import com.giyeok.passzero.Password.Directory
import com.giyeok.passzero.Password.Field
import com.giyeok.passzero.Password.Sheet
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

class PasswordStore(passwordMgr: PasswordManager) {
    def directory(id: String): Directory = ???
    def sheet(id: String): (Sheet, Option[SheetDetail]) = ???
}

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

    private val directoryList = new SortedList[TextSortedListItem[Password.Directory]](getDisplay, this, SWT.NONE)
    directoryList.setLayoutData(fillAll())
    directoryList.addSelectListener({ (selectedOpt, point) =>
        println(point)
        setSelectedDirectory(selectedOpt map { _._1.data })
    })

    private val sheetList = new SortedList[TextSortedListItem[Password.Sheet]](getDisplay, this, SWT.NONE)
    sheetList.setLayoutData(fillAll())
    sheetList.addSelectListener({ (selectedOpt, point) =>
        println(point)
        setSelectedSheet(selectedOpt map { _._1.data })
    })

    private val sheetView = new SheetContentView(config, this, SWT.NONE, this)
    sheetView.setLayoutData(fillAll())
    sheetView.emptyContent()

    private def directoryListItem(directory: Directory) = TextSortedListItem(directory, directory.id, directory.name)
    private def sheetListItem(sheet: Sheet) = TextSortedListItem(sheet, sheet.id, sheet.name)

    private def start(): Unit = {
        directoryList.clear()
        sheetList.clear()
        sheetView.emptyContent()
        session.ensureInitialized() onComplete {
            case Success(_) =>
                getDisplay.syncExec(() =>
                    directoryList.setSource(passwordMgr.directory.directoryList() map { directories =>
                        directories map directoryListItem
                    }))
            case Failure(reason) =>
                reason.printStackTrace()
                getDisplay.syncExec(() => showMessage(reason.getMessage))
        }
    }
    start()

    private def setSelectedDirectory(directoryOpt: Option[Password.Directory]): Unit = {
        println(s"selectedDirectory: $directoryOpt")
        directoryOpt match {
            case Some(directory) =>
                sheetList.setSource(passwordMgr.sheet.sheetList(directory) map { sheets =>
                    sheets map sheetListItem
                })
                sheetView.setDirectory(directory)
            case None =>
                sheetList.clear()
                sheetView.emptyContent()
        }
    }

    private def setSelectedSheet(sheetOpt: Option[Password.Sheet]): Unit = {
        println(s"selectedSheet: $sheetOpt")
        sheetOpt match {
            case Some(sheet) =>
                passwordMgr.sheetDetail.sheetDetail(sheet) foreach { detailOpt =>
                    getDisplay.syncExec(() => {
                        detailOpt match {
                            case None =>
                                sheetView.setSheet(sheet)
                            case Some(detail) =>
                                sheetView.setDetailedSheet(sheet, detail)
                        }
                    })
                }
            case None =>
                sheetView.setDirectory(directoryList.selectedItem.get._1.data)
        }
    }

    def updateDirectory(directory: Directory, name: String): Unit = {
        // TODO 변경되지 않았으면 무시
        passwordMgr.directory.updateDirectory(directory, name) foreach { newDirectoryOpt =>
            newDirectoryOpt foreach { newDirectory =>
                getDisplay.syncExec(() => {
                    directoryList.replaceItem(directory.id, directoryListItem(newDirectory))
                    // sheetList의 데이터 업데이트
                    sheetList transformItems { sheet =>
                        sheetListItem(sheet.data.updateDirectory(newDirectory))
                    }
                })
            }
        }
    }

    def updateSheet(sheet: Sheet, name: String, sheetType: SheetType.Value): Unit = {
        // TODO 변경되지 않았으면 무시
        passwordMgr.sheet.updateSheet(sheet, name, sheetType) foreach { newSheetOpt =>
            newSheetOpt foreach { newSheet =>
                getDisplay.syncExec(() => {
                    sheetList.replaceItem(sheet.id, sheetListItem(newSheet))
                    // sheetView의 데이터 업데이트
                    sheetView.setSheet(sheet)
                })
            }
        }
    }

    def updateSheetDetail(sheet: Sheet, fields: Seq[Field]): Unit = {
        // TODO 변경되지 않았으면 무시
        passwordMgr.sheetDetail.putSheetDetail(sheet, fields) foreach { newSheetDetailOpt =>
            newSheetDetailOpt foreach { newSheetDetail =>
                // TODO sheetView가 보이고 있는 시트가 여전히 sheet인지 확인
                getDisplay.syncExec(() => sheetView.setDetailedSheet(newSheetDetail.sheet, newSheetDetail))
            }
        }
    }
}
