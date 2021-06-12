package com.giyeok.passzero2.ui

import dorkbox.systemTray.MenuItem
import dorkbox.systemTray.SystemTray
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.nio.Buffer
import javax.imageio.ImageIO
import javax.swing.JSeparator
import kotlin.system.exitProcess

class TrayIconManager(
  private val config: Config,
  private val systemTray: SystemTray,
  private val lockedIcon: BufferedImage,
  private val unlockedIcon: BufferedImage
) {
  private var locked: Boolean = true

  private val openWindowMenu: MenuItem
  private val closeSessionMenu: MenuItem
  private val quitMenu: MenuItem

  init {
    openWindowMenu = systemTray.menu.add(MenuItem("Open UI") { })
    closeSessionMenu = systemTray.menu.add(MenuItem("Close Session") {})
    systemTray.menu.add(JSeparator())
    quitMenu = systemTray.menu.add(MenuItem("Quit") { exitProcess(0) })
  }

  fun setLocked() {
    systemTray.setImage(lockedIcon)
    systemTray.status = config.getString("Locked")
    closeSessionMenu.enabled = false
    locked = true
  }

  fun setUnlocked() {
    systemTray.setImage(unlockedIcon)
    systemTray.status = config.getString("Unlocked")
    closeSessionMenu.enabled = true
    locked = false
  }
}
