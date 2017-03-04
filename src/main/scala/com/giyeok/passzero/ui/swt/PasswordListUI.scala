package com.giyeok.passzero.ui.swt

import scala.concurrent.duration._
import scala.util.Random
import com.giyeok.passzero.Session
import com.giyeok.passzero.ui.Config
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label

class PasswordListUI(parent: MainUI, style: Int, session: Session, config: Config)
        extends Composite(parent, style) with ClipboardUtil with FormLayoutUtil {

    val label = new Label(this, SWT.NONE)
    label.setText("PasswordListUI")

    val copyBtn = new Button(this, SWT.NONE)
    copyBtn.setText("Copy")

    copyBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            putTextToClipboard(new String((Random.alphanumeric take 5).toArray), Some(2.seconds))
        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    horizontal(label, copyBtn)
}
