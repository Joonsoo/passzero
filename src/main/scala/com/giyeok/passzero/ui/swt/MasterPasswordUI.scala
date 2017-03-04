package com.giyeok.passzero.ui.swt

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import com.giyeok.passzero.Session
import com.giyeok.passzero.ui.Config
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.ModifyEvent
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text

class MasterPasswordUI(val shell: Shell, parent: MainUI, style: Int, config: Config)
        extends Composite(parent, style) with WidgetUtil with MessageBoxUtil with GridLayoutUtil with FontUtil {
    shell.setText(config.stringRegistry.get("MasterPasswordUI"))

    setLayout(new GridLayout(2, false))

    private val passwordLabel = label(
        "Master Password:",
        horizontalCenter(2) and { _.grabExcessVerticalSpace = true } and { _.verticalAlignment = SWT.BOTTOM }
    )

    private val largerFont = modifyFont(passwordLabel.getFont, 18, SWT.NONE)
    passwordLabel.setFont(largerFont)

    private val password = new Text(this, SWT.PASSWORD | SWT.BORDER)
    password.setFont(largerFont)
    password.setLayoutData(horizontalFill())

    private val passwordLengthLabel = label("0", rightest())
    passwordLengthLabel.setFont(largerFont)

    private val enterBtn = new Button(this, SWT.NONE)
    enterBtn.setText("Enter")
    enterBtn.setFont(largerFont)
    enterBtn.setLayoutData(horizontalCenter(2) and { _.grabExcessVerticalSpace = true } and { _.verticalAlignment = SWT.TOP })

    password.addKeyListener(new KeyListener {
        def keyPressed(e: KeyEvent): Unit = {
            if (e.keyCode == SWT.CR || e.keyCode == SWT.LF) {
                passwordEntered()
            }
        }

        def keyReleased(e: KeyEvent): Unit = {}
    })
    password.addModifyListener((e: ModifyEvent) => {
        passwordLengthLabel.setText(password.getText.length.toString)
        requestLayout()
    })
    enterBtn.addSelectionListener(new SelectionListener {
        def widgetSelected(e: SelectionEvent): Unit = {
            passwordEntered()
        }

        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    })

    private def passwordEntered(): Unit = {
        password.setEnabled(false)
        enterBtn.setEnabled(false)

        val passwordText = password.getText

        // showMessage(s"Loading local info file from ${config.localInfoFile.getCanonicalPath}; ${System.getProperty("java.home")}")

        implicit val ec = ExecutionContext.global
        Future(Try(Session.load(passwordText, config.localInfoFile))) foreach { loadResult =>
            getDisplay.syncExec(() => {
                password.setEnabled(true)
                enterBtn.setEnabled(true)
                loadResult match {
                    case Success(session) =>
                        // showMessage(s"Successfully loaded local info to ${config.localInfoFile.getCanonicalPath}")
                        parent.sessionInitialized(session)
                    case Failure(exception) =>
                        // TODO Illegal Key Size는 특별 처리(설치 방법 안내)
                        exception match {
                            case exception: Exception =>
                                showMessage(s"${exception.getMessage}; ${System.getProperty("java.home")}")
                                password.setFocus()
                        }
                }
            })
        }
    }

    password.setFocus()
}
