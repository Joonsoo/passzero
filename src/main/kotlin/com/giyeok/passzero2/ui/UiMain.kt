package com.giyeok.passzero2.ui

import com.giyeok.passzero2.core.SessionSecret
import io.reactivex.schedulers.Schedulers
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import java.awt.*
import java.io.File
import javax.imageio.ImageIO

open class StringRegistry : (String) -> String {
    override fun invoke(key: String): String = get(key)

    fun get(key: String): String = key
}

class UiConfig(stringRegistry: StringRegistry, val localInfoFile: File) {
    val s = stringRegistry

    companion object {
        val default = UiConfig(StringRegistry(), File("./localInfo.p0"))
    }
}

class UiMain(val config: UiConfig) : Application() {
    constructor() : this(UiConfig.default)

    private lateinit var primaryStage: Stage
    private lateinit var lockedIcon: Image
    private lateinit var unlockedIcon: Image
    private lateinit var trayIcon: TrayIcon

    private val rootStackPane = StackPane()

    override fun start(primaryStage: Stage) {
        this.primaryStage = primaryStage
        Platform.setImplicitExit(false)
        if (initTrayIcon()) {
            startMainUi()
        }
    }

    private fun initTrayIcon(): Boolean {
        Toolkit.getDefaultToolkit()

        return if (SystemTray.isSupported()) {
            val tray = SystemTray.getSystemTray()
            this.lockedIcon = ImageIO.read(javaClass.getResource("/locked.png"))
            this.unlockedIcon = ImageIO.read(javaClass.getResource("/unlocked.png"))
            this.trayIcon = TrayIcon(lockedIcon)

            this.trayIcon.addActionListener { Platform.runLater { this.showPrimaryStage() } }

            val open = MenuItem(config.s("Open UI"))
            open.addActionListener { Platform.runLater { this.showPrimaryStage() } }

            val closeSession = MenuItem(config.s("Close SessionSecret"))
            closeSession.addActionListener { Platform.runLater { this.closeSession() } }

            val quit = MenuItem(config.s("Quit"))
            quit.addActionListener { Platform.exit() }

            val popupMenu = PopupMenu()
            popupMenu.add(open)
            popupMenu.add(closeSession)
            popupMenu.addSeparator()
            popupMenu.add(quit)
            trayIcon.popupMenu = popupMenu

            tray.add(trayIcon)
            true
        } else {
            showMessage(Alert.AlertType.ERROR, config.s("Error"),
                    config.s("System tray is not supported by current platform"))
            Platform.exit()
            false
        }
    }

    fun showMessage(alertType: Alert.AlertType, title: String, text: String) {
        Platform.runLater {
            val alert = Alert(alertType)
            alert.title = title
            alert.contentText = text
            alert.showAndWait()
        }
    }

    private fun showPrimaryStage() {
        primaryStage.show()
        primaryStage.toFront()
        primaryStage.requestFocus()
    }

    private fun startMainUi() {
        primaryStage.title = config.s("Passzero")

        primaryStage.scene = Scene(rootStackPane, 600.0, 480.0)
        primaryStage.x = 100.0
        primaryStage.y = 80.0

        if (config.localInfoFile.isFile && config.localInfoFile.exists()) {
            switchUi(MasterPasswordUi(this).viewRoot(), true)
        } else {
            switchUi(InitializationUi(this).viewRoot(), true)
        }

        showPrimaryStage()
    }

    private fun switchUi(root: Parent, lockedTrayIcon: Boolean) {
        rootStackPane.children.clear()
        rootStackPane.children.add(root)
    }

    fun initSession(sessionSecret: SessionSecret) {
        Platform.runLater {
            val passwordListUi = PasswordListUi(this, sessionSecret, Schedulers.computation())
            val viewRoot = passwordListUi.viewRoot()
            passwordListUi.start()
            this.trayIcon.image = unlockedIcon
            switchUi(viewRoot, false)
        }
    }

    private fun closeSession() {
        startMainUi()
    }
}

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        Application.launch(UiMain::class.java, *args)
        System.exit(0)
    }
}
