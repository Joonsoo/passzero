package com.giyeok.passzero.ui.javafx

import java.io.File
import javafx.application.{Application, Platform}
import javafx.scene.Scene
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.{Alert, Button, Label}
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import javax.imageio.ImageIO

import com.giyeok.passzero.Session
import com.giyeok.passzero.ui.{Config, StringRegistry}

class JavaFxUI extends Application {
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
    def startMainUI(primaryStage: Stage): Unit = {
        primaryStage.setTitle("Passzero")

        // TODO
        val btn = new Button()
        btn.setText("Hello")

        val label = new Label()
        label.setText("Bye~")

        val root = new StackPane()
        root.getChildren.add(btn)

        btn.setOnAction { _ =>
            root.getChildren.clear()
            root.getChildren.add(label)
        }

        primaryStage.setScene(new Scene(root, 500, 320))
        primaryStage.setX(100)
        primaryStage.setY(80)

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
            startMainUI(primaryStage)
        }
    }
}

object JavaFxUI {
    def main(args: Array[String]): Unit = {
        Application.launch(classOf[JavaFxUI], args: _*)
        System.exit(0)
    }
}
