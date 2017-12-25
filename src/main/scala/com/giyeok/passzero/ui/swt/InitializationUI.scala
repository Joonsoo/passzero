//package com.giyeok.passzero.ui.swt
//
//import java.io.File
//import java.security.InvalidKeyException
//import scala.util.Failure
//import scala.util.Success
//import scala.util.Try
//import com.giyeok.passzero.LocalInfo
//import com.giyeok.passzero.LocalSecret
//import com.giyeok.passzero.PasswordUtils
//import com.giyeok.passzero.storage.StorageProfile
//import com.giyeok.passzero.storage.dropbox.DropboxStorageProfile
//import com.giyeok.passzero.storage.googledrive.GoogleDriveStorageProfile
//import com.giyeok.passzero.storage.local.LocalStorageProfile
//import com.giyeok.passzero.ui.Config
//import com.giyeok.passzero.ui.swt.GridLayoutUtil._
//import com.giyeok.passzero.ui.swt.WidgetUtil._
//import org.eclipse.swt.SWT
//import org.eclipse.swt.events.SelectionEvent
//import org.eclipse.swt.events.SelectionListener
//import org.eclipse.swt.graphics.Color
//import org.eclipse.swt.layout.GridLayout
//import org.eclipse.swt.widgets.Button
//import org.eclipse.swt.widgets.Composite
//import org.eclipse.swt.widgets.DirectoryDialog
//import org.eclipse.swt.widgets.FileDialog
//import org.eclipse.swt.widgets.Shell
//import org.eclipse.swt.widgets.TabFolder
//import org.eclipse.swt.widgets.TabItem
//import org.eclipse.swt.widgets.Text
//import org.json4s.native.JsonMethods._
//
//class InitializationUI(val shell: Shell, parent: MainUI, style: Int, config: Config)
//        extends Composite(parent, style) with MessageBoxUtil {
//    // 1. 새로 만들기 시에는
//    //   - MasterPassword와 remote storage 설정을 하면 끝.
//    //   - 설정이 완료되면 config.localInfoFile 위치에 저장하고 parent 상태 업데이트하고
//    //   - Emergency Kit을 pdf 형태로 생성한다(pdfbox 사용) - 저장 및 바로 인쇄 기능
//    // 2. 다른 컴퓨터에서 import 시에는
//    //   - MasterPassword와 remote storage 설정 및 localSecret 코드 입력하면 끝.
//    //   - 여기서 MasterPassword가 틀리면 전체 기능이 제대로 동작하지 않음.
//    // 설정이 완료되면 parent.init() 호출 -> config.localInfoFile 이 생겼으므로 MasterPasswordUI로 넘어간다
//
//    shell.setText(config.stringRegistry.get("InitializationUI"))
//
//    setLayout(new GridLayout(3, false))
//
//    label(this, config.stringRegistry.get("Master Password:"), leftLabel())
//    private val password = new Text(this, SWT.PASSWORD | SWT.BORDER)
//    password.setLayoutData(horizontalFill(2))
//
//    label(this, config.stringRegistry.get("Master Password Confirm:"), leftLabel())
//    private val passwordConfirm = new Text(this, SWT.PASSWORD | SWT.BORDER)
//    passwordConfirm.setLayoutData(horizontalFill(2))
//
//    label(this, config.stringRegistry.get("Revision:"), leftLabel())
//    private val revisionText = text(this, System.currentTimeMillis().toString, horizontalFill(2))
//
//    label(this, config.stringRegistry.get("Secret Key:"), leftLabel())
//    private val secretKeyGenerationCheckBox = button(this, config.stringRegistry.get("New"), SWT.CHECK, leftest())
//    private val secretKey = new Text(this, SWT.BORDER)
//    secretKey.setLayoutData(horizontalFill())
//    secretKey.setEnabled(false)
//    secretKeyGenerationCheckBox.setSelection(true)
//
//    private val tabFolder = new TabFolder(this, SWT.NONE)
//    tabFolder.setLayoutData(fillAll(horizontalSpan = 3))
//
//    private val finishBtn = new Button(this, SWT.NONE)
//    finishBtn.setText("Finish")
//    finishBtn.setLayoutData(rightest(3))
//
//    private def tabItem[T <: Composite](composite: T, title: String): (T, TabItem) = {
//        val tabItem = new TabItem(tabFolder, SWT.NONE)
//        tabItem.setControl(composite)
//        tabItem.setText(title)
//        (composite, tabItem)
//    }
//
//    private val (dropboxProfileTab, _) = tabItem(
//        new DropboxStorageProfileTab(shell, tabFolder, SWT.NONE, config),
//        config.stringRegistry.get("Dropbox")
//    )
//    private val (googleStorageProfileTab, _) = tabItem(
//        new GoogleDriveStorageProfileTab(shell, tabFolder, SWT.NONE, config),
//        config.stringRegistry.get("Google Drive")
//    )
//    private val (localStorageProfileTab, _) = tabItem(
//        new LocalStorageProfileTab(shell, tabFolder, SWT.NONE, config),
//        config.stringRegistry.get("Local Storage")
//    )
//
//    private def createStorageProfile(): StorageProfile = {
//        tabFolder.getSelectionIndex match {
//            case 0 => dropboxProfileTab.storageProfile()
//            case 1 => googleStorageProfileTab.storageProfile()
//            case 2 => localStorageProfileTab.storageProfile()
//        }
//    }
//
//    secretKeyGenerationCheckBox.addSelectionListener(new SelectionListener {
//        def widgetSelected(e: SelectionEvent): Unit = {
//            secretKey.setEnabled(!secretKeyGenerationCheckBox.getSelection)
//        }
//
//        def widgetDefaultSelected(e: SelectionEvent): Unit = {
//            secretKey.setEnabled(!secretKeyGenerationCheckBox.getSelection)
//        }
//    })
//
//    finishBtn.addSelectionListener(new SelectionListener {
//        def widgetSelected(e: SelectionEvent): Unit = {
//            val localSecret: LocalSecret =
//                if (secretKeyGenerationCheckBox.getSelection) {
//                    LocalSecret.generateRandomLocalInfo()
//                } else {
//                    val secretKeyText = secretKey.getText.replaceAllLiterally(" ", "").replaceAllLiterally("-", "").replaceAllLiterally("_", "")
//                    if (secretKeyText forall { LocalSecret.encodingChars contains _ }) {
//                        if (secretKeyText.length == 104) {
//                            LocalSecret.fromAlphaDigits(secretKeyText)
//                        } else {
//                            throw new Exception("Wrong length of secret key text")
//                        }
//                    } else {
//                        throw new Exception("Invalid character in secret key")
//                    }
//                }
//            val revisionOpt: Option[Long] = {
//                val text = revisionText.getText
//                Try(text.toLong) match {
//                    case Failure(reason) =>
//                        showMessage(config.stringRegistry.get(s"Wrong revision: ${reason.getMessage}"))
//                        None
//                    case Success(value) => Some(value)
//                }
//            }
//
//            if (revisionOpt.isDefined && password.getText == passwordConfirm.getText()) {
//                val passwordText = password.getText
//
//                // TODO 복잡도 검사
//                PasswordUtils.checkMasterPassword(passwordText) match {
//                    case Some(reason) =>
//                        showMessage(config.stringRegistry.get(reason))
//                    case None =>
//                        Try(createStorageProfile()) match {
//                            case Success(storageProfile) =>
//                                showMessage(s"Creating local info file to ${config.localInfoFile.getCanonicalPath}; ${System.getProperty("java.home")}")
//                                try {
//                                    val localInfo = new LocalInfo(revisionOpt.get, localSecret, storageProfile)
//                                    println(config.localInfoFile.getCanonicalPath)
//                                    LocalInfo.save(passwordText, localInfo, config.localInfoFile)
//
//                                    parent.init()
//                                    parent.pushEmergencyKit(localInfo)
//                                } catch {
//                                    case exception: InvalidKeyException if exception.getMessage == "Illegal key size" =>
//                                        val message = s"Illegal key size; solution here ${System.getProperty("java.home")}"
//                                        showMessage(message)
//                                    case exception: Exception =>
//                                        val message = s"${exception.getClass.getCanonicalName}: ${exception.getMessage}; ${System.getProperty("java.home")}"
//                                        showMessage(message)
//                                }
//                            case Failure(reason) =>
//                                reason.printStackTrace()
//                                showMessage(s"Failed to create storage profile: ${reason.getMessage}")
//                        }
//                }
//            } else {
//                showMessage(config.stringRegistry.get("password and password confirm does not match"))
//            }
//        }
//
//        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
//    })
//}
//
//class DropboxStorageProfileTab(shell: Shell, parent: TabFolder, style: Int, config: Config) extends Composite(parent, style) {
//    setLayout(new GridLayout(2, false))
//
//    label(this, config.stringRegistry.get("App Name"), leftLabel())
//    private val appNameText = text(this, "passzero", SWT.BORDER, horizontalFill())
//
//    label(this, config.stringRegistry.get("Access Token"), leftLabel())
//    private val accessTokenText = text(this, "", SWT.BORDER, horizontalFill())
//
//    label(this, config.stringRegistry.get("Root Path"), leftLabel())
//    private val rootPathText = text(this, "/passzero", SWT.BORDER, horizontalFill())
//
//    def storageProfile(): DropboxStorageProfile =
//        new DropboxStorageProfile(appNameText.getText, accessTokenText.getText, rootPathText.getText)
//}
//
//class GoogleDriveStorageProfileTab(shell: Shell, parent: TabFolder, style: Int, config: Config) extends Composite(parent, style) {
//    setLayout(new GridLayout(3, false))
//
//    label(this, config.stringRegistry.get("Google Drive AppName"), leftLabel())
//    private val appNameText = new Text(this, SWT.BORDER)
//    appNameText.setLayoutData(horizontalFill(2))
//    appNameText.setText("passzero_app")
//
//    label(this, config.stringRegistry.get("client_secret.json path:"), leftLabel())
//
//    private val clientSecretJsonPath = new Text(this, SWT.READ_ONLY | SWT.BORDER)
//    clientSecretJsonPath.setLayoutData(horizontalFill())
//
//    private val clientSecretJsonDialogButton = new Button(this, SWT.NONE)
//    clientSecretJsonDialogButton.setLayoutData(rightest())
//    clientSecretJsonDialogButton.setText(config.stringRegistry.get("Choose.."))
//
//    clientSecretJsonDialogButton.addSelectionListener(new SelectionListener {
//        def widgetSelected(e: SelectionEvent): Unit = {
//            val fd = new FileDialog(shell, SWT.OPEN)
//            fd.setText(config.stringRegistry.get("client_secret.json"))
//            fd.setFilterExtensions(Seq("*.json").toArray)
//            val selected = fd.open()
//            if (selected != null) {
//                clientSecretJsonPath.setText(selected)
//            }
//        }
//
//        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
//    })
//
//    label(this, config.stringRegistry.get("Application Root"), leftLabel())
//    private val appRootText = new Text(this, SWT.BORDER)
//    appRootText.setLayoutData(horizontalFill(2))
//    appRootText.setText("/passzero")
//
//    def storageProfile(): GoogleDriveStorageProfile = {
//        val applicationName = appNameText.getText
//        if (applicationName.isEmpty) {
//            throw new Exception("application name is empty")
//        }
//        // TODO applicationName 추가 검증
//
//        val applicationRoot = appRootText.getText
//        if (applicationRoot.isEmpty) {
//            throw new Exception("Application root is empty")
//        }
//        // TODO applicationRoot 추가 검증
//
//        val clientSecretJson = clientSecretJsonPath.getText
//        if (clientSecretJson.isEmpty) {
//            throw new Exception("client_secret.json not set")
//        }
//        val clientSecretJsonFile = new File(clientSecretJson)
//        val json = parse(clientSecretJsonFile)
//        new GoogleDriveStorageProfile(applicationName, applicationRoot, json, Map())
//    }
//}
//
//class LocalStorageProfileTab(shell: Shell, parent: Composite, style: Int, config: Config) extends Composite(parent, style) with FontUtil {
//    setLayout(new GridLayout(3, false))
//
//    private val warningLabel = label(this, config.stringRegistry.get("!!! Not Recommended !!!"), horizontalFill(3))
//    private val redColor = new Color(getDisplay, 255, 0, 0)
//    private val warningFont = modifyFont(warningLabel.getFont, 20, SWT.BOLD)
//    warningLabel.setFont(warningFont)
//    warningLabel.setForeground(redColor)
//    warningLabel.setAlignment(SWT.CENTER)
//
//    label(this, config.stringRegistry.get("Root Directory:"), leftLabel())
//    private val rootDirectoryPath = new Text(this, SWT.READ_ONLY | SWT.BORDER)
//    rootDirectoryPath.setLayoutData(horizontalFill())
//
//    private val rootDirectoryDialogButton = new Button(this, SWT.NONE)
//    rootDirectoryDialogButton.setLayoutData(rightest())
//    rootDirectoryDialogButton.setText(config.stringRegistry.get("Choose.."))
//
//    rootDirectoryDialogButton.addSelectionListener(new SelectionListener {
//        def widgetSelected(e: SelectionEvent): Unit = {
//            val dd = new DirectoryDialog(shell, SWT.OPEN)
//            dd.setText(config.stringRegistry.get("Root Directory"))
//            val selected = dd.open()
//            if (selected != null) {
//                rootDirectoryPath.setText(selected)
//            }
//        }
//
//        def widgetDefaultSelected(e: SelectionEvent): Unit = {}
//    })
//
//    def storageProfile(): LocalStorageProfile = {
//        val root = rootDirectoryPath.getText
//        if (root.isEmpty) {
//            throw new Exception(config.stringRegistry.get("root directory is empty"))
//        }
//        val file = new File(root)
//        if (!file.exists()) {
//            throw new Exception("LocalStorage root does not exist!")
//        }
//        if (!file.isDirectory) {
//            throw new Exception("LocalStorage root must be a directory")
//        }
//        new LocalStorageProfile(file)
//    }
//}
