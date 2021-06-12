package com.giyeok.passzero2.ui

import dorkbox.systemTray.SystemTray
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JLabel
import kotlin.system.exitProcess

class MainFrame(private val config: Config, private val trayIconManager: TrayIconManager) :
  JFrame(config.getString("app_title")) {

  init {
    setBounds(100, 80, 600, 480)
    isVisible = true
//    addWindowStateListener { windowsEvent ->
//      println(windowsEvent)
//    }
//    addWindowFocusListener(object : WindowFocusListener {
//      override fun windowGainedFocus(e: WindowEvent?) {
//        println("gained: $e")
//      }
//
//      override fun windowLostFocus(e: WindowEvent?) {
//        println("lost: $e")
//      }
//    })
//    addWindowListener(object : WindowAdapter() {
//      override fun windowClosed(e: WindowEvent?) {
//        println("closed: $e")
//      }
//
//      override fun windowClosing(e: WindowEvent?) {
//        println("closing: $e")
//      }
//    })

    add(JLabel("안뇽하세요"))
  }

  companion object {
    private fun initTray(config: Config): TrayIconManager {
      val systemTray = SystemTray.get()
      if (systemTray == null) {
        println("System tray is not supported. Please try CLI instead")
        exitProcess(1)
      }

      systemTray.installShutdownHook()
      val lockedIcon = ImageIO.read(this::class.java.getResourceAsStream("/locked.png"))
      val unlockedIcon = ImageIO.read(this::class.java.getResourceAsStream("/unlocked.png"))

      val trayIconManager = TrayIconManager(config, systemTray, lockedIcon, unlockedIcon)
      trayIconManager.setLocked()
      return trayIconManager
    }

    @JvmStatic
    fun main(args: Array<String>) {
      val config = Config()

      val trayIconManager = initTray(config)
      MainFrame(config, trayIconManager)
    }
  }
}
