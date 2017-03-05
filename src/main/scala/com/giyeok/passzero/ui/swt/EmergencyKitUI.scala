package com.giyeok.passzero.ui.swt

import com.giyeok.passzero.LocalInfo
import com.giyeok.passzero.ui.Config
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text

class EmergencyKitUI(val shell: Shell, parent: MainUI, style: Int, localInfo: LocalInfo, config: Config)
        extends Composite(parent, style) with WidgetUtil with GridLayoutUtil with FontUtil {
    shell.setText(config.stringRegistry.get("EmergencyKitUI"))
    setLayout(new GridLayout(2, false))

    label("", fillAll(2))

    private val revisionLabel = label("Revision:", leftLabel())
    private val largerFont = modifyFont(revisionLabel.getFont, 14, SWT.NONE)
    revisionLabel.setFont(largerFont)

    private val revisionText = new Text(this, SWT.READ_ONLY)
    revisionText.setLayoutData(horizontalFill())
    revisionText.setText(localInfo.revision.toString)
    revisionText.setFont(largerFont)

    label("", fillAll(2))

    private val secretKeyLabel = label("Secret Key:", leftLabel())
    secretKeyLabel.setFont(largerFont)

    private val secretKey = new Text(this, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP)
    secretKey.setLayoutData(horizontalFill())
    secretKey.setText(localInfo.localSecret.toReadable)
    secretKey.setFont(largerFont)

    label("", fillAll(2))

    private val storageLabel = label("Storage:", leftLabel())
    storageLabel.setFont(largerFont)

    private val storageInfo = new Text(this, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP)
    storageInfo.setLayoutData(horizontalFill())
    storageInfo.setText(localInfo.storageProfile.infoText)
    storageInfo.setFont(largerFont)

    label("", fillAll(2))

    private val pdfBtn = button("PDF")
    pdfBtn.setFont(largerFont)
    pdfBtn.setLayoutData(leftest())

    private val okBtn = button("OK")
    okBtn.setFont(largerFont)
    okBtn.setLayoutData(rightest())

    pdfBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {

        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    okBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            parent.pop()
        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    def escToCloseKeyListener(): KeyListener = new KeyListener {
        def keyPressed(e: KeyEvent): Unit = {
            if (e.keyCode == SWT.ESC) {
                parent.pop()
            }
        }

        def keyReleased(e: KeyEvent): Unit = {}
    }

    addKeyListener(escToCloseKeyListener())
    revisionText.addKeyListener(escToCloseKeyListener())
    secretKey.addKeyListener(escToCloseKeyListener())
    storageInfo.addKeyListener(escToCloseKeyListener())
    pdfBtn.addKeyListener(escToCloseKeyListener())
    okBtn.addKeyListener(escToCloseKeyListener())
}
