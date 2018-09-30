package com.giyeok.passzero.ui.javafx

import java.io.File

import com.giyeok.passzero.Session
import com.giyeok.passzero.ui.javafx.JavaFxUI.View
import com.giyeok.passzero.ui.{Config, StringRegistry}
import javafx.application.{Application, Platform}
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.layout.StackPane
import javafx.scene.{Parent, Scene}
import javafx.stage.Stage
import javax.imageio.ImageIO

class JavaFxUI extends Application {
    // 1. localInfo 파일이 있는지 확인한다
    // 2. 없으면 계정 설정 화면으로 간다. 계정 설정이 완료되면 비밀번호 입력 화면으로 간다
    // 3. 있으면 비밀번호 입력 화면으로 간다
    // 4. 비밀번호가 입력되면 localInfo를 로드한다. 로드하는 도중 오류가 발생하면 다시 반복한다.
    //    별도로 비밀번호가 틀린 것을 검증할 방법은 없고 localInfo 로드가 실패하면 비밀번호가 틀렸거나 localInfo 파일이 손상된 것으로 본다
    // 5. localInfo 로드가 완료되면 session을 생성해서 비밀번호 목록 화면으로 간다.
    // 6. system tray에도 아이콘을 표시하고, 트레이 아이콘을 누르면 UI가 화면에 나온다

    // JavaFX에서 이 클래스 객체를 직접 만들어서 실행하는 방법을 못 찾겠어서 우선 하드코딩
    val config: Config = Config(new StringRegistry {}, new File("./localInfo.p0"))

    private var primaryStage: Stage = _
    private var trayIcon: java.awt.TrayIcon = _
    private var lockedIcon: java.awt.Image = _
    private var unlockedIcon: java.awt.Image = _

    //    private var running: Boolean = true
    //
    //    def quit(): Unit = {
    //        running = false
    //    }
    //
    //    def addTrayItem(display: Display): Option[TrayItem] = {
    //        val tray = display.getSystemTray
    //        if (tray == null) {
    //            None
    //        } else {
    //            val item = new TrayItem(tray, SWT.NONE)
    //            item.setToolTipText("Passzero")
    //
    //            val menu = new Menu(new Shell(display), SWT.POP_UP)
    //
    //            val showUiMenuItem = new MenuItem(menu, SWT.PUSH)
    //            showUiMenuItem.setText(config.stringRegistry.get("Show UI"))
    //            showUiMenuItem.addSelectionListener(new SelectionListener {
    //                override def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    //                override def widgetSelected(e: SelectionEvent): Unit = {
    //                    openMainUI()
    //                }
    //            })
    //
    //            new MenuItem(menu, SWT.SEPARATOR)
    //
    //            val closeSessionMenuItem = new MenuItem(menu, SWT.PUSH)
    //            closeSessionMenuItem.setText(config.stringRegistry.get("Close session"))
    //            closeSessionMenuItem.addSelectionListener(new SelectionListener {
    //                override def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    //                override def widgetSelected(e: SelectionEvent): Unit = {
    //                    closeSession()
    //                }
    //            })
    //
    //            new MenuItem(menu, SWT.SEPARATOR)
    //
    //            val quitMenuItem = new MenuItem(menu, SWT.NONE)
    //            quitMenuItem.setText(config.stringRegistry.get("Quit"))
    //            quitMenuItem.addSelectionListener(new SelectionListener {
    //                override def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    //                override def widgetSelected(e: SelectionEvent): Unit = {
    //                    quit()
    //                }
    //            })
    //
    //            item.addListener(SWT.Selection, (_: Event) => {
    //                // TODO Show PasswordListUI
    //                openMainUI()
    //            })
    //            item.addListener(SWT.MenuDetect, (_: Event) => {
    //                menu.setVisible(true)
    //            })
    //
    //            Some(item)
    //        }
    //    }
    //
    //    private val display = new Display()
    //
    //    private lazy val lockedIcon = new Image(display, getClass.getResourceAsStream("/locked.png"))
    //    private lazy val unlockIcon = new Image(display, getClass.getResourceAsStream("/unlocked.png"))
    //    private var trayItem = Option.empty[TrayItem]
    //
    private var _session = Option.empty[Session]

    def session: Option[Session] = _session

    def initSession(session: Session): Unit = {
        _session = Some(session)
        trayIcon.setImage(unlockedIcon)
        //        mainUi foreach {
        //            _.init()
        //        }
    }

    def closeSession(): Unit = {
        _session = None
        trayIcon.setImage(lockedIcon)
        //        mainUi foreach {
        //            _.init()
        //        }
    }

    //
    //    private var mainUi = Option.empty[MainUI]
    //    private var lastPosition = Option.empty[Rectangle]
    //
    val rootStackPane = new StackPane()

    def switchUi(view: View, lockedTrayIcon: Boolean): Unit = {
        val root = view.viewRoot()
        rootStackPane.getChildren.clear()
        rootStackPane.getChildren.add(root)

        if (lockedTrayIcon) {
            trayIcon.setImage(lockedIcon)
        } else {
            trayIcon.setImage(unlockedIcon)
        }
    }

    def startMainUi(primaryStage: Stage): Unit = {
        primaryStage.setTitle("Passzero")

        primaryStage.setScene(new Scene(rootStackPane, 600, 480))
        primaryStage.setX(100)
        primaryStage.setY(80)

        // localInfo.p0 파일이 있으면 비밀번호 입력 화면으로, 없으면 초기 설정 화면으로
        if (config.localInfoFile.isFile) {
            switchUi(new MasterPasswordUi(this), lockedTrayIcon = true)
        } else {
            switchUi(new InitializationUi(this), lockedTrayIcon = true)
        }

        showPrimaryStage()
    }

    def showPrimaryStage(): Unit = {
        primaryStage.show()
        primaryStage.toFront()
        primaryStage.requestFocus()
    }

    def initTrayIcon(): Boolean = {
        import java.awt._

        java.awt.Toolkit.getDefaultToolkit

        if (SystemTray.isSupported) {
            val tray = SystemTray.getSystemTray
            this.lockedIcon = ImageIO.read(getClass.getResourceAsStream("/locked.png"))
            this.unlockedIcon = ImageIO.read(getClass.getResourceAsStream("/unlocked.png"))
            trayIcon = new TrayIcon(lockedIcon)

            trayIcon addActionListener { _ => Platform runLater { () => this.showPrimaryStage() } }

            val openItem = new MenuItem("Open UI")
            openItem.addActionListener { _ => Platform runLater { () => this.showPrimaryStage() } }

            val closeSessionItem = new MenuItem("Close Session")
            closeSessionItem.addActionListener { _ => Platform runLater { () => this.showPrimaryStage() } }

            val quitItem = new MenuItem("Quit")
            quitItem.addActionListener { _ => Platform.exit() }

            val popupMenu = new PopupMenu()
            popupMenu.add(openItem)
            popupMenu.add(closeSessionItem)
            popupMenu.addSeparator()
            popupMenu.add(quitItem)
            trayIcon.setPopupMenu(popupMenu)

            tray.add(trayIcon)
            true
        } else {
            val alert = new Alert(AlertType.ERROR)
            alert.setTitle("Error")
            alert.setContentText("System tray is not supported by the platform")
            alert.showAndWait()
            Platform.exit()
            false
        }
    }

    override def start(primaryStage: Stage): Unit = {
        this.primaryStage = primaryStage
        Platform.setImplicitExit(false)
        if (initTrayIcon()) {
            startMainUi(primaryStage)
        }
    }

    def showMessage(message: String, alertType: AlertType): Unit = {
        val alert = new Alert(alertType)
        alert.setContentText(message)
        alert.showAndWait()
    }
}

object JavaFxUI {

    trait View {
        def viewRoot(): Parent
    }

    def main(args: Array[String]): Unit = {
        Application.launch(classOf[JavaFxUI], args: _*)
        System.exit(0)
    }
}
