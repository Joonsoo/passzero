package com.giyeok.passzero2.gui.entries

import com.giyeok.passzero2.gui.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.SwingUtilities

class PasswordLabel(config: Config, private val password: String) : JPanel() {
  private val passwordArea = JTextArea()
  private var showingPassword = false

  init {
    layout = GridBagLayout()

    passwordArea.isEditable = false
    passwordArea.text = "****"
    passwordArea.isEnabled = false
    passwordArea.font = config.defaultFont
    var gbc = GridBagConstraints()
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.weightx = 1.0
    gbc.fill = GridBagConstraints.HORIZONTAL
    add(passwordArea, gbc)

    val showButton = JButton()
    showButton.text = "Show"
    showButton.font = config.defaultFont

    showButton.addActionListener {
      showingPassword = !showingPassword
      if (showingPassword) {
        passwordArea.text = password
        passwordArea.isEnabled = true
      } else {
        passwordArea.text = "****"
        passwordArea.isEnabled = false
      }
    }

    gbc = GridBagConstraints()
    gbc.gridx = 1
    gbc.gridy = 0
    add(showButton, gbc)

    val copyButton = JButton()
    copyButton.text = "Copy"
    copyButton.font = config.defaultFont

    copyButton.addActionListener {
      val clipboard = Toolkit.getDefaultToolkit().systemClipboard
      val copied = StringSelection(password)
      SwingUtilities.invokeLater {
        clipboard.setContents(copied, null)
      }
      CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
        delay(30 * 1000) // 30 secs
        SwingUtilities.invokeLater {
          val current = clipboard.getContents(null)
          if (current?.getTransferData(DataFlavor.stringFlavor) == password) {
            clipboard.setContents(object : Transferable {
              override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf()
              override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean = false
              override fun getTransferData(flavor: DataFlavor?): Any = TODO()
            }, null)
          }
        }
      }
    }

    gbc = GridBagConstraints()
    gbc.gridx = 2
    gbc.gridy = 0
    add(copyButton, gbc)
  }
}