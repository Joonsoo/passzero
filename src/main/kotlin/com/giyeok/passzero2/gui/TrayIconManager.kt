package com.giyeok.passzero2.gui

import dorkbox.systemTray.MenuItem
import dorkbox.systemTray.SystemTray
import java.awt.image.BufferedImage
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

  var openUiListener: (() -> Unit)? = null
  var closeSessionListener: (() -> Unit)? = null

  init {
    openWindowMenu = systemTray.menu.add(MenuItem("Open UI") { openUiListener?.let { it() } })
    closeSessionMenu = systemTray.menu.add(MenuItem("Close Session") { closeSessionListener?.let { it() } })
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
