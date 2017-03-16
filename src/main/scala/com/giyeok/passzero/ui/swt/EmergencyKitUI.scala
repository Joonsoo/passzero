package com.giyeok.passzero.ui.swt

import java.io.File
import com.giyeok.passzero.LocalInfo
import com.giyeok.passzero.ui.Config
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.FileDialog
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text

// TODO secret key를 master password로 한번 암호화해서 표시하자
class EmergencyKitUI(val shell: Shell, parent: MainUI, style: Int, localInfo: LocalInfo, config: Config)
        extends Composite(parent, style) with WidgetUtil with GridLayoutUtil with FontUtil with MessageBoxUtil {
    shell.setText(config.stringRegistry.get("EmergencyKitUI"))
    setLayout(new GridLayout(2, false))

    label(this, "", fillAll(2))

    private val revisionLabel = label(this, "Revision:", leftLabel())
    private val largerFont = modifyFont(revisionLabel.getFont, 14, SWT.NONE)
    revisionLabel.setFont(largerFont)

    private val revisionText = new Text(this, SWT.READ_ONLY)
    revisionText.setLayoutData(horizontalFill())
    revisionText.setText(localInfo.revision.toString)
    revisionText.setFont(largerFont)

    label(this, "", fillAll(2))

    private val secretKeyLabel = label(this, "Secret Key:", leftLabel())
    secretKeyLabel.setFont(largerFont)

    private val secretKey = new Text(this, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP)
    secretKey.setLayoutData(horizontalFill())
    secretKey.setText(localInfo.localSecret.toReadable)
    secretKey.setFont(largerFont)

    label(this, "", fillAll(2))

    private val storageLabel = label(this, "Storage:", leftLabel())
    storageLabel.setFont(largerFont)

    private val storageInfo = new Text(this, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP)
    storageInfo.setLayoutData(horizontalFill())
    storageInfo.setText(localInfo.storageProfile.infoText)
    storageInfo.setFont(largerFont)

    label(this, "", fillAll(2))

    private val pdfBtn = button(this, "PDF")
    pdfBtn.setFont(largerFont)
    pdfBtn.setLayoutData(leftest())

    private val okBtn = button(this, "OK")
    okBtn.setFont(largerFont)
    okBtn.setLayoutData(rightest())

    pdfBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            val fd = new FileDialog(shell, SWT.SAVE)
            fd.setText(config.stringRegistry.get("emergency-kit.pdf"))
            fd.setFileName("emergency-kit.pdf")
            fd.setFilterExtensions(Seq("*.pdf").toArray)
            val selected = fd.open()
            if (selected != null) {
                try {
                    new EmergencyKitPdf(localInfo).saveTo(new File(selected))
                } catch {
                    case exception: Throwable =>
                        showMessage(exception.getMessage)
                }
            }
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

class EmergencyKitPdf(localInfo: LocalInfo) {
    val document: PDDocument = new PDDocument()

    {
        val page = new PDPage()

        val stream = new PDPageContentStream(document, page)

        def addLines(lines: Seq[String], font: PDFont, fontSize: Float, tx: Float, ty: Float, lineHeight: Float): Unit = {
            lines.foldLeft(ty) { (y, line) =>
                stream.beginText()
                stream.setFont(font, fontSize)
                stream.newLineAtOffset(tx, y)
                stream.showText(line)
                stream.endText()
                y - lineHeight
            }
        }
        def addText(text: String, font: PDFont, fontSize: Float, tx: Float, ty: Float, lineHeight: Float): Unit = {
            val lines = (text.split('\n') flatMap { _.split('\r') }) filter { _.nonEmpty }
            addLines(lines, font, fontSize, tx, ty, lineHeight)
        }

        val fontSize = 25

        addText("Passzero Emergency Kit", PDType1Font.HELVETICA_BOLD, fontSize, 25, 700, fontSize)

        addText("Revision:", PDType1Font.HELVETICA, fontSize, 25, 600, fontSize)
        addText(localInfo.revision.toString, PDType1Font.COURIER, fontSize, 150, 600, fontSize)

        addText("Secret:", PDType1Font.HELVETICA, fontSize, 25, 500, 30)
        val secretKeyText = {
            val readable = localInfo.localSecret.toAlphaDigits
            ((readable grouped 8) grouped 3 map { x =>
                x mkString "-"
            }).toSeq
        }
        addLines(secretKeyText, PDType1Font.COURIER, fontSize, 150, 500, fontSize)

        // TODO Revision 및 Secret 정보에 대한 QR 코드 추가

        addText("Storage:", PDType1Font.HELVETICA, fontSize, 25, 300, 30)
        addText(localInfo.storageProfile.infoText, PDType1Font.COURIER, fontSize, 150, 300, fontSize)

        addText("Master Password:", PDType1Font.HELVETICA, fontSize, 25, 200, fontSize)

        stream.close()

        document.addPage(page)
    }

    def saveTo(file: File): Unit = {
        document.save(file)
    }

    def printTo(): Unit = {
        ???
    }
}
