package com.giyeok.passzero.ui.javafx

import javafx.application.Platform
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.Parent
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.{Alert, Button, Label, PasswordField}
import javafx.scene.input.KeyCode
import javafx.scene.layout.Pane

import com.giyeok.passzero.Session

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

class MasterPasswordController {
    @FXML
    var passwordLengthLabel: Label = _
    @FXML
    var passwordField: PasswordField = _
    @FXML
    var systemInfoLabel: Label = _
    @FXML
    var passwordSubmit: Button = _
}

class MasterPasswordUi(mainUi: JavaFxUI) extends JavaFxUI.View {
    private var view: Pane = _
    private var controller: MasterPasswordController = _

    override def viewRoot(): Parent = {
        val loader = new FXMLLoader(getClass.getResource("/views/masterPassword.fxml"))
        view = loader.load.asInstanceOf[Pane]
        controller = loader.getController.asInstanceOf[MasterPasswordController]

        // controller.firstLine.prefWidthProperty().bind(view.widthProperty())
        controller.systemInfoLabel.setText(
            s"""java home: ${System.getProperty("java.home")}
               |local info: ${mainUi.config.localInfoFile.getCanonicalPath}
             """.stripMargin.trim)
        controller.passwordLengthLabel.textProperty().bind(controller.passwordField.textProperty().length().asString())

        controller.passwordField.setOnKeyPressed { event =>
            if (event.getCode == KeyCode.ENTER) {
                tryPassword(controller.passwordField.getText)
            }
        }
        controller.passwordSubmit.setOnAction { event =>
            tryPassword(controller.passwordField.getText)
        }

        view
    }

    def setEnabledAll(enabled: Boolean): Unit = {
        controller.passwordField.setDisable(!enabled)
        controller.passwordSubmit.setDisable(!enabled)
    }

    def tryPassword(passwordText: String): Unit = {
        setEnabledAll(false)

        implicit val ec: ExecutionContextExecutor = ExecutionContext.global
        Future(Try(Session.load(passwordText, mainUi.config.localInfoFile))) foreach { loadResult =>
            Platform.runLater { () =>
                loadResult match {
                    case Success(session) =>
                        // showMessage(s"Successfully loaded local info to ${config.localInfoFile.getCanonicalPath}")
                        // TODO
                        println(session)
                    case Failure(exception) =>
                        // TODO Illegal Key Size는 특별 처리(설치 방법 안내)
                        val message = exception match {
                            case invalidKeyException: java.security.InvalidKeyException =>
                                invalidKeyException.printStackTrace()
                                // showMessage(s"${throwable.getMessage}; ${System.getProperty("java.home")}")
                                // http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html
                                s"Error while loading.\nIt seems to be JCE problem${System.getProperty("java.home")}"
                            case throwable: Throwable =>
                                throwable.printStackTrace()
                                // showMessage(s"${throwable.getMessage}; ${System.getProperty("java.home")}")
                                s"Error while loading. Check your password again\n${throwable.getMessage}\n${System.getProperty("java.home")}"
                        }
                        showMessage(message)
                        setEnabledAll(true)
                        controller.passwordField.requestFocus()
                }
            }
        }
    }

    def showMessage(message: String): Unit = {
        val alert = new Alert(AlertType.ERROR)
        alert.setContentText(message)
        alert.showAndWait()
    }
}
