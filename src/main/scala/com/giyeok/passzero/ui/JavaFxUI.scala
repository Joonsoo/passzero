package com.giyeok.passzero.ui

import javafx.application.Application
import javafx.stage.Stage

class JavaFxUI(config: Config) extends Application {
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
    //    private var _session = Option.empty[Session]
    //
    //    def session: Option[Session] = _session
    //
    //    def initSession(session: Session): Unit = {
    //        _session = Some(session)
    //        trayItem foreach { _.setImage(unlockIcon) }
    //        mainUi foreach { _.init() }
    //    }
    //
    //    def closeSession(): Unit = {
    //        _session = None
    //        trayItem foreach { _.setImage(lockedIcon) }
    //        mainUi foreach { _.init() }
    //    }
    //
    //    private var mainUi = Option.empty[MainUI]
    //    private var lastPosition = Option.empty[Rectangle]
    //
    //    def openMainUI(): Unit = {
    //        mainUi match {
    //            case Some(ui) if !ui.isDisposed =>
    //                val shell = ui.getShell
    //                shell.setMinimized(false)
    //                shell.setActive()
    //                shell.forceActive()
    //                shell.setFocus()
    //                shell.forceFocus()
    //            case _ =>
    //                val shell = new Shell(display)
    //
    //                lastPosition match {
    //                    case Some(rectangle) => shell.setBounds(rectangle)
    //                    case None => shell.setBounds(50, 50, 600, 500)
    //                }
    //                shell.setText(config.stringRegistry.get("MainTitle"))
    //
    //                shell.setLayout(new FillLayout())
    //                val newUi = new MainUI(this, shell, SWT.NONE, config)
    //
    //                shell.addListener(SWT.Resize, (e: Event) => {
    //                    lastPosition = Some(shell.getBounds)
    //                })
    //                shell.addListener(SWT.Move, (e: Event) => {
    //                    lastPosition = Some(shell.getBounds)
    //                })
    //
    //                mainUi = Some(newUi)
    //
    //                shell.open()
    //        }
    //    }

    override def start(primaryStage: Stage): Unit = {
        //        trayItem = addTrayItem(display)
        //        trayItem match {
        //            case Some(item) =>
        //                item.setImage(lockedIcon)
        //            case None => // 어쩌지?
        //        }
        //
        //        openMainUI()
        //
        //        while (running && !display.isDisposed) {
        //            if (!display.readAndDispatch()) {
        //                display.sleep()
        //            }
        //        }
        //        if (!display.isDisposed) {
        //            display.dispose()
        //        }
    }
}
