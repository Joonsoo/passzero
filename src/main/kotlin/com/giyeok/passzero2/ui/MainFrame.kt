package com.giyeok.passzero2.ui

import dorkbox.systemTray.SystemTray
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.JFrame
import javax.swing.JLabel

class MainFrame(private val config: Config) :
  JFrame(config.getString("app_title")) {

  init {
    add(JLabel("안뇽하세요"))
  }
}

fun main() {
  val systemTray = SystemTray.get()
  if (systemTray == null) {

  }

  val mainFrame = MainFrame(Config())
  mainFrame.setBounds(100, 80, 600, 480)
  mainFrame.isVisible = true
  mainFrame.addWindowStateListener { windowsEvent ->
    println(windowsEvent)
  }
  mainFrame.addWindowFocusListener(object : WindowFocusListener {
    override fun windowGainedFocus(e: WindowEvent?) {
      println("gained: $e")
    }

    override fun windowLostFocus(e: WindowEvent?) {
      println("lost: $e")
    }
  })
  mainFrame.addWindowListener(object : WindowAdapter() {
    override fun windowClosed(e: WindowEvent?) {
      println("closed: $e")
    }

    override fun windowClosing(e: WindowEvent?) {
      println("closing: $e")
    }
  })
}
