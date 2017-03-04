package com.giyeok.passzero.ui

import java.security.InvalidKeyException
import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Random
import scala.util.Success
import scala.util.Try
import com.giyeok.passzero.LocalInfo
import com.giyeok.passzero.LocalSecret
import com.giyeok.passzero.Session
import com.giyeok.passzero.storage.memory.MemoryStorageProfile
import org.eclipse.swt.SWT
import org.eclipse.swt.dnd.Clipboard
import org.eclipse.swt.dnd.TextTransfer
import org.eclipse.swt.events.DisposeEvent
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.MessageBox
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text

object SWTUI {
    def start(config: Config): Unit = {
        val display = new Display()
        val shell = new Shell(display)

        shell.setBounds(50, 50, 500, 400)
        shell.setText(config.stringRegistry.get("MainTitle"))

        shell.setLayout(new FillLayout())
        new MainUI(shell, SWT.NONE, config)

        shell.open()

        while (!shell.isDisposed) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        display.close()
    }

    class MainUI(parent: Composite, style: Int, config: Config) extends Composite(parent, style) {
        // 1. localInfo 파일이 있는지 확인한다
        // 2. 없으면 계정 설정 화면으로 간다. 계정 설정이 완료되면 비밀번호 입력 화면으로 간다
        // 3. 있으면 비밀번호 입력 화면으로 간다
        // 4. 비밀번호가 입력되면 localInfo를 로드한다. 로드하는 도중 오류가 발생하면 다시 반복한다.
        //    별도로 비밀번호가 틀린 것을 검증할 방법은 없고 localInfo 로드가 실패하면 비밀번호가 틀렸거나 localInfo 파일이 손상된 것으로 본다
        // 5. localInfo 로드가 완료되면 session을 생성해서 비밀번호 목록 화면으로 간다.
        // 6. system tray에도 아이콘을 표시하고, 트레이 아이콘을 누르면 UI가 화면에 나온다
        // - MainUI의 레이아웃은 FillLayout으로 하고 상태가 변경되면 기존의 컨트롤은 dispose한다

        setLayout(new FillLayout)

        def replaceChild(contentFunc: MainUI => Control): Unit = {
            getChildren foreach { _.dispose() }
            contentFunc(this)
            requestLayout()
        }

        def init(): Unit = {
            if (!config.localInfoFile.exists()) {
                replaceChild(new SettingUI(getShell, _, SWT.NONE, config))
            } else {
                replaceChild(new MasterPasswordUI(getShell, _, SWT.NONE, config))
            }
        }

        def sessionInitialized(session: Session): Unit = {
            replaceChild(new PasswordListUI(_, SWT.NONE, session, config))
        }

        init()
    }

    sealed trait MessageBoxable extends Control {
        val shell: Shell

        def showMessage(message: String, style: Int = SWT.NONE): Unit = {
            val box = new MessageBox(shell, style)
            box.setMessage(message)
            box.open()
        }
    }

    trait FormLayoutable extends Composite {
        def horizontal(controls: Control*): Unit = {
            setLayout(new FormLayout)

            if (controls.nonEmpty) {
                val fd = new FormData
                fd.top = new FormAttachment(0, 0)
                fd.left = new FormAttachment(0, 10)
                fd.bottom = new FormAttachment(0, 20)
                // fd.right = new FormAttachment(0, 0)
                controls.head.setLayoutData(fd)

                controls.tail.foldLeft(controls.head) { (prev, ctrl) =>
                    val fd = new FormData
                    fd.top = new FormAttachment(0, 0)
                    fd.left = new FormAttachment(prev, 10)
                    fd.bottom = new FormAttachment(0, 20)
                    ctrl.setLayoutData(fd)
                    ctrl
                }
            }
        }
    }

    trait ExpirableClipboard extends Control {
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

    class SettingUI(val shell: Shell, parent: MainUI, style: Int, config: Config)
            extends Composite(parent, style) with MessageBoxable with FormLayoutable {
        // 1. 새로 만들기 시에는
        //   - MasterPassword와 remote storage 설정을 하면 끝.
        //   - 설정이 완료되면 config.localInfoFile 위치에 저장하고 parent 상태 업데이트하고
        //   - Emergency Kit을 pdf 형태로 생성한다(pdfbox 사용) - 저장 및 바로 인쇄 기능
        // 2. 다른 컴퓨터에서 import 시에는
        //   - MasterPassword와 remote storage 설정 및 localSecret 코드 입력하면 끝.
        //   - 여기서 MasterPassword가 틀리면 전체 기능이 제대로 동작하지 않음.
        // 설정이 완료되면 parent.init() 호출 -> config.localInfoFile 이 생겼으므로 MasterPasswordUI로 넘어간다

        val label = new Label(this, SWT.NONE)
        label.setText("SettingUI")

        val password = new Text(this, SWT.PASSWORD | SWT.BORDER)

        val passwordConfirm = new Text(this, SWT.PASSWORD | SWT.BORDER)

        val enterBtn = new Button(this, SWT.NONE)
        enterBtn.setText("Enter")
        enterBtn.addSelectionListener(new SelectionListener {
            def widgetSelected(e: SelectionEvent): Unit = {
                println(password.getText())

                showMessage(s"Creating local info file to ${config.localInfoFile.getCanonicalPath}; ${System.getProperty("java.home")}")
                try {
                    val localInfo = new LocalInfo(LocalSecret.generateRandomLocalInfo(), new MemoryStorageProfile)
                    println(config.localInfoFile.getCanonicalPath)
                    LocalInfo.save(password.getText(), localInfo, config.localInfoFile)

                    parent.init()
                } catch {
                    case exception: InvalidKeyException if exception.getMessage == "Illegal key size" =>
                        val message = s"Illegal key size; solution here ${System.getProperty("java.home")}"
                        showMessage(message)
                    case exception: Exception =>
                        val message = s"${exception.getClass.getCanonicalName}: ${exception.getMessage}; ${System.getProperty("java.home")}"
                        showMessage(message)
                }
            }

            def widgetDefaultSelected(e: SelectionEvent): Unit = {}
        })

        horizontal(label, password, passwordConfirm, enterBtn)
    }

    class MasterPasswordUI(val shell: Shell, parent: MainUI, style: Int, config: Config)
            extends Composite(parent, style) with MessageBoxable with FormLayoutable {
        val label = new Label(this, SWT.NONE)
        label.setText("MasterPasswordUI")

        val password = new Text(this, SWT.PASSWORD | SWT.BORDER)

        val enterBtn = new Button(this, SWT.NONE)
        enterBtn.setText("Enter")
        enterBtn.addSelectionListener(new SelectionListener {
            def widgetSelected(e: SelectionEvent): Unit = {
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
                                }
                        }
                    })
                }
            }

            def widgetDefaultSelected(e: SelectionEvent): Unit = {}
        })

        horizontal(label, password, enterBtn)
    }

    class PasswordListUI(parent: MainUI, style: Int, session: Session, config: Config)
            extends Composite(parent, style) with ExpirableClipboard with FormLayoutable {

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
}
