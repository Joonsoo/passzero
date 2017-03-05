package com.giyeok.passzero.ui.swt

import scala.concurrent.duration._
import scala.util.Random
import com.giyeok.passzero.Session
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.ui.Config
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Shell
import com.giyeok.passzero.utils.ByteArrayUtil._

class PasswordListUI(private val shell: Shell, parent: MainUI, style: Int, session: Session, config: Config)
        extends Composite(parent, style) with WidgetUtil with ClipboardUtil with GridLayoutUtil {
    shell.setText(config.stringRegistry.get("PasswordListUI"))
    setLayout(new GridLayout(3, false))

    private val saveBtn = button("Save")
    private val loadBtn = button("Load")
    private val copyBtn = button("Copy")

    saveBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            session.put(Path("hello?"), "helloworld".toBytes)
        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    loadBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            val list = session.list(Path(""))
            list foreach println
        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    copyBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            putTextToClipboard(new String((Random.alphanumeric take 5).toArray), Some(30.seconds))
        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })
}
