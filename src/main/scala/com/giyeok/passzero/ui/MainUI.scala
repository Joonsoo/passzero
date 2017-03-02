package com.giyeok.passzero.ui

import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import scala.concurrent.duration._
import com.giyeok.passzero.Session

object MainUI {
    def main(args: Array[String]): Unit = {
        val config: Config = Config(new StringRegistry {}, new File("./localInfo"))

        val frame = new JFrame(config.stringRegistry.get("MainTitle"))
        frame.setSize(800, 600)
        frame.add(new MainUI(config))
        frame.setVisible(true)
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    }
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
        add(content, layoutConstraint)
    }

    if (!config.localInfoFile.exists()) {
        replaceChild(new SettingUI(config, this))
    } else {
        replaceChild(new MasterPasswordUI(config, this))
    }
}

class SettingUI(config: Config, parent: MainUI) extends JPanel {
    add(new JLabel("SettingUI"))

    val btn = new JButton("Hello?")
    add(btn)

    btn.addActionListener((e: ActionEvent) => {
        new PasswordListUI(null, config, parent).putTextToClipboard("What??", None)
    })

    private val myFont = new JLabel().getFont

    override def paintComponent(g: Graphics): Unit = {
        super.paintComponent(g)

        val g2 = g.asInstanceOf[Graphics2D]
        g2.drawRect(0, 0, getWidth - 1, getHeight - 1)
        g2.drawLine(0, 0, 1000, 1000)
        g2.setFont(new Font("Monospaced", Font.PLAIN, 100))
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.drawString("Hello??", 100, 100)
    }
}

class MasterPasswordUI(config: Config, parent: MainUI) extends JPanel {
    add(new JLabel("MasterPasswordUI"))
}

class PasswordListUI(session: Session, config: Config, parent: MainUI) extends JPanel {
    // session 객체와 session.localInfo 객체는 절대 이 클래스 밖으로 나가서는 안된다.

    def putTextToClipboard(text: String, timeoutOpt: Option[Duration]): Unit = {
        val clipboard = Toolkit.getDefaultToolkit.getSystemClipboard
        clipboard.setContents(new StringSelection(text), null)
        timeoutOpt foreach { timeout =>
            // TODO timeout이 Some이면 그 시간만큼 지난 후에 clipboard 지우기
            ???
        }
    }
}
