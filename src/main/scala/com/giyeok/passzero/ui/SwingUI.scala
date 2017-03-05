package com.giyeok.passzero.ui

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.ActionEvent
import java.security.InvalidKeyException
import java.util.concurrent.atomic.AtomicLong
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.Timer
import javax.swing.UIManager
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Random
import scala.util.Success
import scala.util.Try
import com.giyeok.passzero.LocalInfo
import com.giyeok.passzero.LocalSecret
import com.giyeok.passzero.Session
import com.giyeok.passzero.storage.memory.MemoryStorageProfile

object SwingUI {
    def start(config: Config): Unit = {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName)

        val frame = new JFrame(config.stringRegistry.get("MainTitle"))
        frame.setLocation(200, 200)
        frame.setSize(800, 600)
        frame.add(new SwingUI.MainUI(config))
        frame.setVisible(true)
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    }

    class MainUI(config: Config) extends JPanel {
        // 1. localInfo 파일이 있는지 확인한다
        // 2. 없으면 계정 설정 화면으로 간다. 계정 설정이 완료되면 비밀번호 입력 화면으로 간다
        // 3. 있으면 비밀번호 입력 화면으로 간다
        // 4. 비밀번호가 입력되면 localInfo를 로드한다. 로드하는 도중 오류가 발생하면 다시 반복한다.
        //    별도로 비밀번호가 틀린 것을 검증할 방법은 없고 localInfo 로드가 실패하면 비밀번호가 틀렸거나 localInfo 파일이 손상된 것으로 본다
        // 5. localInfo 로드가 완료되면 session을 생성해서 비밀번호 목록 화면으로 간다.
        // 6. system tray에도 아이콘을 표시하고, 트레이 아이콘을 누르면 UI가 화면에 나온다
        // - MainUI의 레이아웃은 FillLayout으로 하고 상태가 변경되면 기존의 컨트롤은 dispose한다

        setLayout(new GridBagLayout)

        private def replaceChild(content: JPanel): Unit = {
            val layoutConstraint = new GridBagConstraints()
            layoutConstraint.fill = GridBagConstraints.BOTH
            layoutConstraint.weightx = 1.0
            layoutConstraint.weighty = 1.0

            removeAll()
            add(content, layoutConstraint)

            revalidate()
            repaint()
        }

        def init(): Unit = {
            if (!config.localInfoFile.exists()) {
                replaceChild(new SettingUI(config, this))
            } else {
                replaceChild(new MasterPasswordUI(config, this))
            }
        }

        def sessionInitialized(session: Session): Unit = {
            replaceChild(new PasswordListUI(session, config, this))
        }

        init()
    }

    class SettingUI(config: Config, parent: MainUI) extends JPanel {
        // 1. 새로 만들기 시에는
        //   - MasterPassword와 remote storage 설정을 하면 끝.
        //   - 설정이 완료되면 config.localInfoFile 위치에 저장하고 parent 상태 업데이트하고
        //   - Emergency Kit을 pdf 형태로 생성한다(pdfbox 사용) - 저장 및 바로 인쇄 기능
        // 2. 다른 컴퓨터에서 import 시에는
        //   - MasterPassword와 remote storage 설정 및 localSecret 코드 입력하면 끝.
        //   - 여기서 MasterPassword가 틀리면 전체 기능이 제대로 동작하지 않음.
        // 설정이 완료되면 parent.init() 호출 -> config.localInfoFile 이 생겼으므로 MasterPasswordUI로 넘어간다

        add(new JLabel("SettingUI"))

        val passwordBox = new JPasswordField(20)
        add(passwordBox)

        val passwordConfirmBox = new JPasswordField(20)
        add(passwordConfirmBox)

        val btn = new JButton("Save")
        add(btn)

        btn.addActionListener((e: ActionEvent) => {
            val password = new String(passwordBox.getPassword)

            JOptionPane.showMessageDialog(this, s"Creating local info file to ${config.localInfoFile.getCanonicalPath}; ${System.getProperty("java.home")}")
            try {
                val localInfo = new LocalInfo(System.currentTimeMillis(), LocalSecret.generateRandomLocalInfo(), new MemoryStorageProfile)
                println(config.localInfoFile.getCanonicalPath)
                LocalInfo.save(password, localInfo, config.localInfoFile)

                new PasswordListUI(null, config, parent).putTextToClipboard("What??", None)

                parent.init()
            } catch {
                case exception: InvalidKeyException if exception.getMessage == "Illegal key size" =>
                    val message = s"Illegal key size; solution here ${System.getProperty("java.home")}"
                    JOptionPane.showMessageDialog(this, message)
                case exception: Exception =>
                    val message = s"${exception.getClass.getCanonicalName}: ${exception.getMessage}; ${System.getProperty("java.home")}"
                    JOptionPane.showMessageDialog(this, message)
            }
        })

        //    private val myFont = new JLabel().getFont
        //
        //    override def paintComponent(g: Graphics): Unit = {
        //        super.paintComponent(g)
        //
        //        val g2 = g.asInstanceOf[Graphics2D]
        //        g2.drawRect(0, 0, getWidth - 1, getHeight - 1)
        //        g2.drawLine(0, 0, 1000, 1000)
        //        g2.setFont(new Font("Monospaced", Font.PLAIN, 100))
        //        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        //        g2.drawString("Hello??", 100, 100)
        //    }
    }

    class MasterPasswordUI(config: Config, parent: MainUI) extends JPanel {
        add(new JLabel("MasterPasswordUI"))

        val passwordBox = new JPasswordField(20)
        add(passwordBox)

        val enterBtn = new JButton("Enter")
        add(enterBtn)

        enterBtn.addActionListener({ (e: ActionEvent) =>
            passwordEntered(new String(passwordBox.getPassword))
        })

        def passwordEntered(password: String): Unit = {
            JOptionPane.showMessageDialog(this, s"Loading local info file from ${config.localInfoFile.getCanonicalPath}; ${System.getProperty("java.home")}")
            Try(Session.load(password, config.localInfoFile)) match {
                case Success(session) =>
                    parent.sessionInitialized(session)
                    JOptionPane.showMessageDialog(this, s"Successfully loaded local info to ${config.localInfoFile.getCanonicalPath}")
                case Failure(exception) =>
                    // TODO Illegal Key Size는 특별 처리
                    exception match {
                        case exception: Exception =>
                            val message = s"${exception.getMessage}; ${System.getProperty("java.home")}"
                            JOptionPane.showMessageDialog(this, message)
                    }
            }
        }
    }

    class PasswordListUI(session: Session, config: Config, parent: MainUI) extends JPanel with ExpirableClipboard {
        // session 객체와 session.localInfo 객체는 절대 이 클래스 밖으로 나가서는 안된다.

        add(new JLabel("PasswordListUI"))

        val copyBtn = new JButton("Copy")
        add(copyBtn)

        copyBtn.addActionListener({ (e: ActionEvent) =>
            putTextToClipboard(new String((Random.alphanumeric take 5).toArray), Some(30.seconds))
        })
    }

    trait ExpirableClipboard {
        private var lastClipboardPut = Option.empty[(Long, String)]

        private val putIdCounter = new AtomicLong()

        class EmptyTransferable extends Transferable {
            def getTransferData(flavor: DataFlavor): AnyRef =
                throw new UnsupportedFlavorException(flavor)

            def getTransferDataFlavors: Array[DataFlavor] =
                new Array[DataFlavor](0)

            def isDataFlavorSupported(flavor: DataFlavor): Boolean = false
        }

        def putTextToClipboard(text: String, timeoutOpt: Option[Duration]): Unit = {
            val clipboard = Toolkit.getDefaultToolkit.getSystemClipboard
            clipboard.setContents(new StringSelection(text), null)
            timeoutOpt foreach { timeout =>
                // timeout이 Some이면 그 시간만큼 지난 후에 clipboard 지우기
                val putId = putIdCounter.incrementAndGet()
                lastClipboardPut = Some(putId, text)
                println(lastClipboardPut)
                val timer = new Timer(timeout.toMillis.toInt, { (_: ActionEvent) =>
                    this.synchronized {
                        println(s"Removing $putId $lastClipboardPut")
                        lastClipboardPut match {
                            case Some((`putId`, lastText)) =>
                                val contents = clipboard.getContents(null)
                                Try(contents.getTransferData(DataFlavor.stringFlavor)) match {
                                    case Success(`lastText`) =>
                                        clipboard.setContents(new EmptyTransferable, null)
                                        println("Clipboard cleared")
                                    case _ => // nothing to do
                                }
                            case _ => // nothing to do
                        }
                    }
                })
                timer.setRepeats(false)
                timer.start()
            }
        }
    }
}
