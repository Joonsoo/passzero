package com.giyeok.passzero2.gui

import com.giyeok.passzero2.core.CryptSession
import com.giyeok.passzero2.core.LocalInfoWithRevision
import com.giyeok.passzero2.core.storage.DropboxSession
import com.google.protobuf.ByteString
import okhttp3.OkHttpClient
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

class MasterPasswordView(
  private val config: Config,
  private val appStateManager: AppStateManager,
  private val okHttpClient: OkHttpClient
) :
  JPanel() {
  private val systemInfoText = JTextArea()
  private val masterPassword = JPasswordField()
  private val lengthLabel = JLabel("0")
  private val confirmButton = JButton(config.getString("Confirm"))

  init {
    layout = GridBagLayout()

    var gbc = GridBagConstraints()
    gbc.gridx = 0
    gbc.gridy = 1
    gbc.weightx = 1.0
    gbc.fill = GridBagConstraints.HORIZONTAL
    masterPassword.font = config.bigFont
    add(masterPassword, gbc)

    gbc = GridBagConstraints()
    gbc.gridx = 0
    gbc.gridy = 2
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.anchor = GridBagConstraints.EAST
    lengthLabel.font = config.defaultFont
    add(lengthLabel, gbc)

    gbc = GridBagConstraints()
    gbc.gridx = 1
    gbc.gridy = 1
    gbc.fill = GridBagConstraints.HORIZONTAL
    confirmButton.font = config.defaultFont
    add(confirmButton, gbc)

    gbc = GridBagConstraints()
    gbc.gridx = 0
    gbc.gridy = 4
    gbc.gridwidth = 2
    gbc.fill = GridBagConstraints.BOTH
    systemInfoText.font = config.defaultFont
    systemInfoText.isEditable = false
    systemInfoText.text = """
      java home: ${System.getProperty("java.home")}
      local info: ${config.localInfoFile.canonicalPath}
    """.trimIndent()
    add(systemInfoText, gbc)

    val spacer1 = JPanel()
    gbc = GridBagConstraints()
    gbc.gridx = 0
    gbc.gridy = 3
    gbc.weighty = 1.0
    gbc.fill = GridBagConstraints.VERTICAL
    add(spacer1, gbc)

    val spacer2 = JPanel()
    gbc = GridBagConstraints()
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.weighty = 1.0
    gbc.fill = GridBagConstraints.VERTICAL
    add(spacer2, gbc)

    masterPassword.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        SwingUtilities.invokeLater {
          lengthLabel.text = "${masterPassword.password.size}"
        }
        if (e.keyCode == KeyEvent.VK_ENTER) {
          tryPassword()
        }
      }
    })
    confirmButton.addActionListener { tryPassword() }
  }

  fun tryPassword() {
    SwingUtilities.invokeLater {
      masterPassword.isEnabled = false
      confirmButton.isEnabled = false
    }
    val password = String(masterPassword.password)
    val session = try {
      val localInfo = LocalInfoWithRevision.decode(
        password,
        ByteString.readFrom(config.localInfoFile.inputStream())
      )
      val cryptSession = CryptSession.from(localInfo.localInfoWithRevision, password)
      DropboxSession(
        cryptSession,
        localInfo.localInfoWithRevision.localInfo.storageProfile.dropbox,
        okHttpClient
      )
    } catch (e: Exception) {
      SwingUtilities.invokeLater {
        JOptionPane.showMessageDialog(null, e.message)
        masterPassword.isEnabled = true
        confirmButton.isEnabled = true
      }
      e.printStackTrace()
      null
    }
    if (session != null) {
      appStateManager.sessionReady(session)
    }
  }
}
