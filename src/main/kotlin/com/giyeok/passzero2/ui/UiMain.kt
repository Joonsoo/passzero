package com.giyeok.passzero2.ui

import com.giyeok.passzero2.core.SessionSecret
import dorkbox.systemTray.MenuItem
import dorkbox.systemTray.SystemTray
import io.reactivex.schedulers.Schedulers
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import java.awt.Image
import java.awt.Toolkit
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
  private lateinit var systemTray: SystemTray

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

    val systemTray = SystemTray.get()
    if (systemTray == null) {
      showMessage(
        Alert.AlertType.ERROR, config.s("Error"),
        config.s("System tray is not supported by current platform")
      )
      Platform.exit()
      return false
    }

    this.systemTray = systemTray
    systemTray.installShutdownHook()

    this.lockedIcon = ImageIO.read(javaClass.getResource("/locked.png"))
    this.unlockedIcon = ImageIO.read(javaClass.getResource("/unlocked.png"))
    this.systemTray.setImage(lockedIcon)

    val trayMenu = this.systemTray.menu
    trayMenu.add(MenuItem("Open UI") { Platform.runLater { this.showPrimaryStage() } })
    trayMenu.add(MenuItem("Close Session") { Platform.runLater { this.closeSession() } })
    trayMenu.add(MenuItem("Quit") { Platform.exit() })
    return true
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
      this.systemTray.setImage(unlockedIcon)
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
