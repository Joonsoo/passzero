package com.giyeok.passzero.ui.swt

import scala.concurrent.duration._
import scala.util.Random
import com.giyeok.passzero.Session
import com.giyeok.passzero.ui.Config
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Shell

class PasswordListUI(private val shell: Shell, parent: MainUI, style: Int, session: Session, config: Config)
        extends Composite(parent, style) with ClipboardUtil with GridLayoutUtil {
    shell.setText(config.stringRegistry.get("PasswordListUI"))
    setLayout(new GridLayout(3, false))

    val copyBtn = new Button(this, SWT.NONE)
    copyBtn.setText("Copy")

    copyBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            putTextToClipboard(new String((Random.alphanumeric take 5).toArray), Some(30.seconds))
        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })
}
