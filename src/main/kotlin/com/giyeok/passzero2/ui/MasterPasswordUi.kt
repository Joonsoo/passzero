package com.giyeok.passzero2.ui

import com.giyeok.passzero2.core.Session
import io.reactivex.Single
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.layout.Pane
import java.security.InvalidKeyException

class MasterPasswordController {
    @FXML
    lateinit var passwordLengthLabel: Label
    @FXML
    lateinit var passwordField: PasswordField
    @FXML
    lateinit var systemInfoLabel: TextArea
    @FXML
    lateinit var passwordSubmit: Button
}

class MasterPasswordUi(private val main: UiMain) {
    private lateinit var view: Pane
    private lateinit var controller: MasterPasswordController

    fun viewRoot(): Parent {
        val loader = FXMLLoader(javaClass.getResource("/views/masterPassword.fxml"))
        view = loader.load()
        controller = loader.getController()

        controller.systemInfoLabel.text = """
            |Java Home: ${System.getProperty("java.home")}
            |Local Info: ${main.config.localInfoFile.canonicalPath}
        """.trimMargin()

        controller.passwordLengthLabel.textProperty().bind(controller.passwordField.textProperty().length().asString())

        controller.passwordField.setOnKeyPressed { event ->
            if (event.code == KeyCode.ENTER) {
                tryPassword(controller.passwordField.text)
            }
        }
        controller.passwordSubmit.setOnAction { event ->
            tryPassword(controller.passwordField.text)
        }

        return view
    }

    private fun setEnabledAll(enabled: Boolean) {
        controller.passwordField.isDisable = !enabled
        controller.passwordSubmit.isDisable = !enabled
    }

    private fun tryPassword(passwordText: String) {
        setEnabledAll(false)

        Single.create<Session> { Session.load(passwordText, main.config.localInfoFile) }.subscribe { session, error ->
            if (error == null) {
                main.initSession(session)
            } else {
                val message: String = when (error) {
                    is InvalidKeyException -> {
                        error.printStackTrace()
                        """${main.config.s("Error while loading")}
                            |${main.config.s("It seems to be JCE problem")}
                            |java.home=${System.getProperty("java.home")}""".trimMargin()
                    }
                    else -> {
                        error.printStackTrace()
                        """${main.config.s("Error while loading")}
                            |${main.config.s("Check your password again")}
                            |error=${error.message}
                            |java.home=${System.getProperty("java.home")}
                        """.trimMargin()
                    }
                }
                main.showMessage(Alert.AlertType.ERROR, main.config.s("Error"), message)
            }
        }
    }
}
