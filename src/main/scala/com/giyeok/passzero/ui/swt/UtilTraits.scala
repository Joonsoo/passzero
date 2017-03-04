package com.giyeok.passzero.ui.swt

import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.duration.Duration
import org.eclipse.swt.SWT
import org.eclipse.swt.dnd.Clipboard
import org.eclipse.swt.dnd.TextTransfer
import org.eclipse.swt.events.DisposeEvent
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.FontData
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.MessageBox
import org.eclipse.swt.widgets.Shell

trait WidgetUtil extends Composite {
    def label(text: String, style: Int = SWT.NONE, layoutData: AnyRef = null): Label = {
        val label = new Label(this, style)
        label.setText(text)
        if (layoutData != null) {
            label.setLayoutData(layoutData)
        }
        label
    }
    def label(text: String, layoutData: AnyRef): Label =
        label(text, style = SWT.NONE, layoutData = layoutData)
}

trait MessageBoxUtil extends Control {
    val shell: Shell

    def showMessage(message: String, style: Int = SWT.NONE): Unit = {
        val box = new MessageBox(shell, style)
        box.setMessage(message)
        box.open()
    }
}

trait GridLayoutUtil extends Composite {
    def leftLabel(): GridData = {
        val gd = new GridData()
        gd.horizontalAlignment = SWT.RIGHT
        gd
    }

    def leftest(horizontalSpan: Int = -1): GridData = {
        val gd = new GridData()
        gd.horizontalAlignment = SWT.LEFT
        if (horizontalSpan >= 0) {
            gd.horizontalSpan = horizontalSpan
        }
        gd
    }

    def rightest(horizontalSpan: Int = -1): GridData = {
        val gd = new GridData()
        gd.horizontalAlignment = SWT.RIGHT
        if (horizontalSpan >= 0) {
            gd.horizontalSpan = horizontalSpan
        }
        gd
    }

    def horizontalFill(horizontalSpan: Int = -1): GridData = {
        val gd = new GridData()
        if (horizontalSpan >= 0) {
            gd.horizontalSpan = horizontalSpan
        }
        gd.horizontalAlignment = SWT.FILL
        gd.grabExcessHorizontalSpace = true
        gd
    }

    def fillAll(horizontalSpan: Int = -1, verticalSpan: Int = -1): GridData = {
        val gd = new GridData()
        if (horizontalSpan >= 0) {
            gd.horizontalSpan = horizontalSpan
        }
        if (verticalSpan >= 0) {
            gd.verticalSpan = verticalSpan
        }
        gd.horizontalAlignment = SWT.FILL
        gd.verticalAlignment = SWT.FILL
        gd.grabExcessHorizontalSpace = true
        gd.grabExcessVerticalSpace = true
        gd
    }

    def horizontalCenter(horizontalSpan: Int = -1): GridData = {
        val gd = new GridData()
        if (horizontalSpan >= 0) {
            gd.horizontalSpan = horizontalSpan
        }
        gd.horizontalAlignment = SWT.CENTER
        gd.grabExcessHorizontalSpace = true
        gd
    }

    implicit class GridDataAdd(gd: GridData) {
        def and(additional: GridData => Unit): GridData = {
            additional(gd)
            gd
        }

        def exclusiveTop(): GridData = {
            gd.grabExcessVerticalSpace = true
            gd.verticalAlignment = SWT.BOTTOM
            gd
        }

        def exclusiveBottom(): GridData = {
            gd.grabExcessVerticalSpace = true
            gd.verticalAlignment = SWT.TOP
            gd
        }
    }
}

trait FormLayoutUtil extends Composite {
    def horizontal(controls: Control*): Unit = {
        setLayout(new FormLayout)

        if (controls.nonEmpty) {
            controls.head.setLayoutData(formData(
                left = att(0, 10), top = att(0, 0), bottom = att(0, 20)
            ))

            controls.tail.foldLeft(controls.head) { (prev, ctrl) =>
                ctrl.setLayoutData(formData(
                    left = att(prev), top = att(0, 0), bottom = att(0, 20)
                ))
                ctrl
            }
        }
    }

    // Option으로 하면 외부에서 사용할 때 불편해서 null 사용함
    def formData(left: FormAttachment = null, top: FormAttachment = null, right: FormAttachment = null, bottom: FormAttachment = null): FormData = {
        val fd = new FormData
        if (left != null) {
            fd.left = left
        }
        if (top != null) {
            fd.top = top
        }
        if (right != null) {
            fd.right = right
        }
        if (bottom != null) {
            fd.bottom = bottom
        }
        fd
    }

    def att() = new FormAttachment()
    def att(numerator: Int) = new FormAttachment(numerator)
    def att(numerator: Int, offset: Int) = new FormAttachment(numerator, offset)
    def att(numerator: Int, denominator: Int, offset: Int) = new FormAttachment(numerator, denominator, offset)
    def att(control: Control) = new FormAttachment(control)
    def att(control: Control, offset: Int) = new FormAttachment(control, offset)
    def att(control: Control, offset: Int, alignment: Int) = new FormAttachment(control, offset, alignment)
}

trait ClipboardUtil extends Control {
    private var lastClipboardPut = Option.empty[(Long, String)]

    private val putIdCounter = new AtomicLong()

    lazy val clipboard = new Clipboard(getDisplay)

    addDisposeListener((_: DisposeEvent) => {
        clipboard.dispose()
    })

    def putTextToClipboard(text: String, timeoutOpt: Option[Duration]): Unit = {
        clipboard.setContents(Seq(text).toArray, Seq(TextTransfer.getInstance).toArray)
        timeoutOpt foreach { timeout =>
            // timeout이 Some이면 그 시간만큼 지난 후에 clipboard 지우기
            val putId = putIdCounter.incrementAndGet()
            lastClipboardPut = Some(putId, text)
            // println(lastClipboardPut)
            getDisplay.timerExec(timeout.toMillis.toInt, () => {
                this.synchronized {
                    // println(s"Removing $putId $lastClipboardPut")
                    lastClipboardPut match {
                        case Some((`putId`, lastText)) =>
                            clipboard.getContents(TextTransfer.getInstance) match {
                                case `lastText` =>
                                    clipboard.clearContents()
                                    println("Clipboard cleared")
                                case _ => // nothing to do
                            }
                        case _ => // nothing to do
                    }
                }
            })
        }
    }
}

object ColorUtil {
    lazy val redColor = new Color(null, 255, 0, 0)
}

trait FontUtil extends Control {
    def modifyFont(font: Font, size: Int, newStyle: Int): Font = {
        val fd = font.getFontData
        fd(0).setHeight(size)
        fd(0).setStyle(newStyle)
        new Font(getDisplay, fd)
    }
}
