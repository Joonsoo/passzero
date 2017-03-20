package com.giyeok.passzero.ui

import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.MenuItem
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.TrayItem

class SWTUI(config: Config) {
    private var running: Boolean = true

    def quit(): Unit = {
        running = false
    }

    def addTrayItem(display: Display): Option[TrayItem] = {
        val tray = display.getSystemTray
        if (tray == null) {
            None
        } else {
            val item = new TrayItem(tray, SWT.NONE)
            item.setToolTipText("Passzero")

            val image = new Image(display, 16, 16)
            item.setImage(image)

            val menu = new Menu(new Shell(display), SWT.POP_UP)
            (0 until 8) foreach { i =>
                val menuItem = new MenuItem(menu, SWT.PUSH)
                menuItem.setText(s"item $i")
            }

            val quitMenuItem = new MenuItem(menu, SWT.NONE)
            quitMenuItem.setText(config.stringRegistry.get("Quit"))
            quitMenuItem.addSelectionListener(new SelectionListener {
                override def widgetDefaultSelected(e: SelectionEvent): Unit = {}
                override def widgetSelected(e: SelectionEvent): Unit = {
                    quit()
                }
            })

            item.addListener(SWT.Selection, (_: Event) => {
                // TODO Show PasswordListUI
                // 이미 뜬게 있으면 그거에 setFocus
                // 없으면 새로 띄움
            })
            item.addListener(SWT.MenuDetect, (_: Event) => {
                menu.setVisible(true)
            })

            Some(item)
        }
    }

    def start(): Unit = {
        val display = new Display()

        addTrayItem(display) match {
            case Some(_) => // nothing
            case None => // 어쩌지?
        }

        val shell = new Shell(display)

        shell.setBounds(50, 50, 600, 500)
        shell.setText(config.stringRegistry.get("MainTitle"))

        shell.setLayout(new FillLayout())
        new swt.MainUI(shell, SWT.NONE, config)

        shell.open()

        while (running && !display.isDisposed) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        if (!display.isDisposed) {
            display.dispose()
        }
    }
}
