package com.giyeok.passzero.ui

import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell

object SWTUI {
    def start(config: Config): Unit = {
        val display = new Display()
        val shell = new Shell(display)

        shell.setBounds(50, 50, 500, 400)
        shell.setText(config.stringRegistry.get("MainTitle"))

        shell.setLayout(new FillLayout())
        new swt.MainUI(shell, SWT.NONE, config)

        shell.open()

        while (!shell.isDisposed) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        display.close()
    }
}
