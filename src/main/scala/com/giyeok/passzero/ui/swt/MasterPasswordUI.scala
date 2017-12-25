//package com.giyeok.passzero.ui.swt
//
//import scala.concurrent.ExecutionContext
//import scala.concurrent.Future
//import scala.util.Failure
//import scala.util.Success
//import scala.util.Try
//import com.giyeok.passzero.Session
//import com.giyeok.passzero.ui.Config
//import com.giyeok.passzero.ui.swt.GridLayoutUtil._
//import com.giyeok.passzero.ui.swt.WidgetUtil._
//import org.eclipse.swt.SWT
//import org.eclipse.swt.events.KeyEvent
//import org.eclipse.swt.events.KeyListener
//import org.eclipse.swt.events.ModifyEvent
//import org.eclipse.swt.events.SelectionEvent
//import org.eclipse.swt.events.SelectionListener
//import org.eclipse.swt.layout.GridLayout
//import org.eclipse.swt.widgets.Button
//import org.eclipse.swt.widgets.Composite
//import org.eclipse.swt.widgets.Shell
//import org.eclipse.swt.widgets.Text
//
//class MasterPasswordUI(val shell: Shell, parent: MainUI, style: Int, config: Config)
//        extends Composite(parent, style) with MessageBoxUtil with FontUtil {
//    shell.setText(config.stringRegistry.get("MasterPasswordUI"))
//
//    setLayout(new GridLayout(2, false))
//
//    private val passwordLabel = label(this, config.stringRegistry.get("Master Password:"), leftest().exclusiveTop())
//    private val largerFont = modifyFont(passwordLabel.getFont, 18, SWT.NONE)
//
//    passwordLabel.setFont(largerFont)
//
//    private val passwordLengthLabel = label(this, "0", SWT.RIGHT, horizontalFill().exclusiveTop())
//    passwordLengthLabel.setFont(largerFont)
//
//    private val password = new Text(this, SWT.PASSWORD | SWT.BORDER)
//    password.setFont(largerFont)
//    password.setLayoutData(horizontalFill(2))
//
//    private val enterBtn = new Button(this, SWT.NONE)
//    enterBtn.setText(config.stringRegistry.get("Enter"))
//    enterBtn.setFont(largerFont)
//    enterBtn.setLayoutData(rightest(2).exclusiveBottom())
//
//    password.addKeyListener(new KeyListener {
//        def keyPressed(e: KeyEvent): Unit = {
//            if (e.keyCode == SWT.CR || e.keyCode == SWT.LF) {
//                passwordEntered()
//            }
//        }
//        def keyReleased(e: KeyEvent): Unit = {}
//    })
//    password.addModifyListener((e: ModifyEvent) => {
//        passwordLengthLabel.setText(password.getText.length.toString)
//        requestLayout()
//    })
//    enterBtn.addSelectionListener(new SelectionListener {
//        def widgetSelected(e: SelectionEvent): Unit = {
//            passwordEntered()
//        }
//
//        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
//    })
//
//    private def allChildrenSetEnabled(enabled: Boolean): Unit = {
//        getChildren foreach { _.setEnabled(enabled) }
//    }
//
//    private def passwordEntered(): Unit = {
//        allChildrenSetEnabled(false)
//
//        val passwordText = password.getText
//
//        // showMessage(s"Loading local info file from ${config.localInfoFile.getCanonicalPath}; ${System.getProperty("java.home")}")
//
//        implicit val ec = ExecutionContext.global
//        Future(Try(Session.load(passwordText, config.localInfoFile))) foreach { loadResult =>
//            getDisplay.syncExec(() => {
//                allChildrenSetEnabled(true)
//                loadResult match {
//                    case Success(session) =>
//                        // showMessage(s"Successfully loaded local info to ${config.localInfoFile.getCanonicalPath}")
//                        parent.sessionInitialized(session)
//                    case Failure(exception) =>
//                        // TODO Illegal Key Size는 특별 처리(설치 방법 안내)
//                        exception match {
//                            case invalidKeyException: java.security.InvalidKeyException =>
//                                invalidKeyException.printStackTrace()
//                                // showMessage(s"${throwable.getMessage}; ${System.getProperty("java.home")}")
//                                // http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html
//                                showMessage(config.stringRegistry.get(s"Error while loading.\nIt seems to be JCE problem${System.getProperty("java.home")}"))
//                                password.setFocus()
//                            case throwable: Throwable =>
//                                throwable.printStackTrace()
//                                // showMessage(s"${throwable.getMessage}; ${System.getProperty("java.home")}")
//                                showMessage(config.stringRegistry.get(s"Error while loading. Check your password again\n${throwable.getMessage}\n${System.getProperty("java.home")}"))
//                                password.setFocus()
//                        }
//                }
//            })
//        }
//    }
//
//    password.setFocus()
//}
