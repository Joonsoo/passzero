package com.giyeok.passzero2.gui

import com.giyeok.passzero2.gui.entries.EntryListView
import dorkbox.systemTray.SystemTray
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

class Main(
  private val config: Config,
  private val appStateManager: AppStateManager,
  private val trayIconManager: TrayIconManager
) : JFrame(config.getString("app_title")) {

  init {
    setBounds(100, 80, 800, 600)
    isVisible = true
    trayIconManager.openUiListener = {
      isVisible = true
    }
    trayIconManager.closeSessionListener = {
      appStateManager.closeSession()
    }
    CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
      appStateManager.state.collect { state ->
        val locked = when (state) {
          AppStateManager.LocalInfoNotExists, AppStateManager.PasswordNotReady -> true
          is AppStateManager.SessionReady -> false
        }
        val p0Config =
          if (state is AppStateManager.SessionReady) state.session.getConfig() else null
        SwingUtilities.invokeLater {
          val content = when (state) {
            AppStateManager.LocalInfoNotExists -> LocalInfoInitView(config, appStateManager)
            AppStateManager.PasswordNotReady -> MasterPasswordView(config, appStateManager)
            is AppStateManager.SessionReady ->
              EntryListView(config, state.session, p0Config!!.defaultDirectory)
          }
          contentPane = content
          this@Main.revalidate()
          this@Main.repaint()
          if (locked) {
            trayIconManager.setLocked()
          } else {
            trayIconManager.setUnlocked()
          }
        }
      }
    }
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

      return TrayIconManager(config, systemTray, lockedIcon, unlockedIcon)
    }

    @JvmStatic
    fun main(args: Array<String>) {
      val parser = ArgParser("passzero-gui")
      val localInfoPath by parser.option(ArgType.String, "localInfo", "l", "Path to local info")
        .default("./localInfo.p0")

      parser.parse(args)

      val config = Config(File(localInfoPath), Executors.newFixedThreadPool(4))
      val appStateManager = AppStateManager(config)
      val trayIconManager = initTray(config)
      Main(config, appStateManager, trayIconManager)
    }
  }
}
