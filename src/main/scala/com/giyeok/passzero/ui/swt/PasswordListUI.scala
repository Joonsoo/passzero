package com.giyeok.passzero.ui.swt

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Random
import scala.util.Success
import com.giyeok.passzero.Password.DirectoryId
import com.giyeok.passzero.Password.DirectoryInfo
import com.giyeok.passzero.Password.Field
import com.giyeok.passzero.Password.SheetDetail
import com.giyeok.passzero.Password.SheetId
import com.giyeok.passzero.Password.SheetInfo
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
    def directory(id: DirectoryId): Future[Option[DirectoryInfo]] = passwordMgr.directory.get(id)
    def sheet(id: SheetId): Future[Option[(SheetInfo, Option[SheetDetail])]] = {
        implicit val ec = ExecutionContext.global
        passwordMgr.sheet.get(id) flatMap {
            case Some(info) =>
                passwordMgr.sheetDetail.sheetDetail(id) map { detail =>
                    Some(info, detail)
                }
            case None => Future.successful(None)
        }
    }
}

class PasswordListUI(val shell: Shell, parent: MainUI, style: Int, session: Session, config: Config)
        extends Composite(parent, style) with WidgetUtil with ClipboardUtil with GridLayoutUtil with MessageBoxUtil {
    implicit private val ec = ExecutionContext.global

    private val passwordMgr = new PasswordManager(session)
    private val passwordStore = new PasswordStore(passwordMgr)

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
            directoryList.selectedItem foreach { selected =>
                val directoryId = selected._1
                passwordMgr.sheet.createSheet(directoryId, s"amazon.com ${Math.abs(Random.nextInt())}", SheetType.Login)
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

    private val directoryList = new SortedList[DirectoryId, TextSortedListItem[DirectoryInfo]](getDisplay, this, SWT.NONE, id => {
        passwordStore.directory(id) map { d => TextSortedListItem(d.get, d.get.name) }
    })
    directoryList.setLayoutData(fillAll())
    directoryList.addSelectListener({ (selectedOpt, point) =>
        println(point)
        setSelectedDirectory(selectedOpt map { s => (s._1, s._2.data) })
    })

    private val sheetList = new SortedList[SheetId, TextSortedListItem[SheetInfo]](getDisplay, this, SWT.NONE, id => {
        passwordStore.sheet(id) map { s => TextSortedListItem(s.get._1, s.get._1.name) }
    })
    sheetList.setLayoutData(fillAll())
    sheetList.addSelectListener({ (selectedOpt, point) =>
        println(point)
        setSelectedSheet(selectedOpt map { _._1 })
    })

    private val sheetView = new SheetContentView(config, this, SWT.NONE, this)
    sheetView.setLayoutData(fillAll())
    sheetView.emptyContent()

    private def directoryListItem(id: DirectoryId, info: DirectoryInfo) = TextSortedListItem(info, info.name)
    private def sheetListItem(id: SheetId, info: SheetInfo) = TextSortedListItem(info, info.name)

    private def start(): Unit = {
        directoryList.clear()
        sheetList.clear()
        sheetView.emptyContent()
        session.ensureInitialized() onComplete {
            case Success(_) =>
                getDisplay.syncExec(() =>
                    directoryList.setSource(passwordMgr.directory.directoryList() map { directories =>
                        directories map { p => (p._1, directoryListItem(p._1, p._2)) }
                    }))
            case Failure(reason) =>
                reason.printStackTrace()
                getDisplay.syncExec(() => showMessage(reason.getMessage))
        }
    }
    start()

    private def setSelectedDirectory(directoryOpt: Option[(DirectoryId, DirectoryInfo)]): Unit = {
        println(s"selectedDirectory: $directoryOpt")
        directoryOpt match {
            case Some((directoryId, directoryInfo)) =>
                sheetList.setSource(passwordMgr.sheet.sheetList(directoryId) map { sheets =>
                    sheets map { s => (s._1, sheetListItem(s._1, s._2)) }
                })
                sheetView.setDirectory(directoryId, directoryInfo)
            case None =>
                sheetList.clear()
                sheetView.emptyContent()
        }
    }

    private def setSelectedSheet(sheetOpt: Option[SheetId]): Unit = {
        println(s"selectedSheet: $sheetOpt")
        sheetOpt foreach { sheet =>
            passwordMgr.sheetDetail.sheetDetail(sheet) foreach { detailOpt =>
                getDisplay.syncExec(() => {
                    sheetView.setSheetDetail(sheet, detailOpt)
                })
            }
        }
    }

    def updateDirectory(directoryId: DirectoryId, name: String): Unit = {
        // TODO 변경되지 않았으면 무시
        passwordMgr.directory.updateDirectory(directoryId, name) foreach { newDirectoryOpt =>
            newDirectoryOpt foreach { newDirectory =>
                getDisplay.syncExec(() => {
                    directoryList.refreshAllItems()
                    sheetList.refreshAllItems()
                })
            }
        }
    }

    def updateSheet(sheetId: SheetId, name: String, sheetType: SheetType.Value): Unit = {
        // TODO 변경되지 않았으면 무시
        passwordMgr.sheet.updateSheet(sheetId, name, sheetType) foreach { newSheetOpt =>
            newSheetOpt foreach { newSheet =>
                getDisplay.syncExec(() => {
                    sheetList.refreshAllItems()
                    // sheetView의 데이터 업데이트
                    sheetView.refreshSheet()
                })
            }
        }
    }

    def updateSheetDetail(sheetId: SheetId, fields: Seq[Field]): Unit = {
        // TODO 변경되지 않았으면 무시
        passwordMgr.sheetDetail.putSheetDetail(sheetId, fields) foreach { newSheetDetailOpt =>
            // TODO sheetView가 보이고 있는 시트가 여전히 sheet인지 확인
            getDisplay.syncExec(() => sheetView.refreshSheet())
        }
    }
}
