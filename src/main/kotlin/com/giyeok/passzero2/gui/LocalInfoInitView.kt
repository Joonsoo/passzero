package com.giyeok.passzero2.gui

import com.giyeok.passzero2.core.*
import com.giyeok.passzero2.core.LocalInfoProto.*
import com.giyeok.passzero2.core.storage.DropboxSession
import com.giyeok.passzero2.core.storage.DropboxToken
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.protobuf.ByteString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.security.SecureRandom
import java.util.*
import javax.swing.*

class LocalInfoInitView(
  private val config: Config,
  private val appStateManager: AppStateManager,
  private val okHttpClient: OkHttpClient,
) : JPanel() {

  private val state: LocalInfoInitViewState
  private val stateMutex = Mutex()
  private val stateFlow = MutableSharedFlow<LocalInfoInitViewState>(1)

  private fun setState(stateUpdater: LocalInfoInitViewState.() -> Unit) {
    runBlocking {
      stateMutex.withLock {
        stateUpdater(state)
      }
      stateFlow.emit(state)
    }
  }

  private val localSecretText = JTextArea()
  private val regenLocalSecret = JButton(config.getString("REGENERATE_LOCAL_SECRET"))
  private val dropboxAppKey = JTextField()
  private val dropboxRedirectUri = JTextField()
  private val dropboxAuthUri = JTextArea()
  private val dropboxAccessCode = JTextField()
  private val dropboxTokenStatus = JTextField()
  private val dropboxAppRootPath = JTextField()
  private val masterPassword = JPasswordField()
  private val masterPasswordVerify = JPasswordField()
  private val generateLocalInfo = JButton(config.getString("GENERATE_LOCAL_INFO"))

  private val gson =
    GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

  private fun gbcLabel(y: Int): GridBagConstraints {
    val gbc = GridBagConstraints()
    gbc.gridx = 0
    gbc.gridy = y
    gbc.insets = Insets(3, 10, 3, 5)
    return gbc
  }

  private fun gbcBody(y: Int): GridBagConstraints {
    val gbc = GridBagConstraints()
    gbc.gridx = 1
    gbc.gridy = y
    gbc.weightx = 1.0
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.insets = Insets(3, 5, 3, 10)
    return gbc
  }

  private fun label(stringKey: String): JLabel {
    val l = JLabel(config.getString(stringKey))
    l.font = config.defaultFont
    return l
  }

  init {
    val r = SecureRandom()
    val choices = (('a'..'z').toSet() + ('A'..'Z') + ('0'..'9')).toCharArray()
    val codeVerifier = (0 until 100).map {
      choices[r.nextInt(choices.size)]
    }.toCharArray().concatToString()
    val codeChallenge = Base64.getEncoder().withoutPadding().encodeToString(
      Encryption.SHA256.encode(ByteString.copyFrom(codeVerifier, "US-ASCII")).toByteArray()
    )
      .replace('+', '-')
      .replace('/', '_')
      .replace(Regex("=\\+\\$"), "")

    state = LocalInfoInitViewState(
      Encryption.generateLocalSecret(),
      DropboxState(
        "hgndvjtq97o4jls",
        "https://giyeok.com/passzero_dropbox_oauth",
        codeVerifier,
        codeChallenge,
        "",
        MutableStateFlow(null),
        "/passzero",
      ),
      masterPasswordOk = false,
      masterPasswordVerifyOk = true
    )

    runBlocking {
      stateFlow.emit(state)
    }

    localSecretText.isEditable = false
    dropboxAuthUri.isEditable = false
    dropboxTokenStatus.isEditable = false
    localSecretText.font = config.defaultFont
    regenLocalSecret.font = config.defaultFont
    dropboxAppKey.font = config.defaultFont
    dropboxRedirectUri.font = config.defaultFont
    dropboxAuthUri.font = config.defaultFont
    dropboxAccessCode.font = config.defaultFont
    dropboxAppRootPath.font = config.defaultFont
    masterPassword.font = config.defaultFont
    masterPasswordVerify.font = config.defaultFont
    generateLocalInfo.font = config.defaultFont

    layout = GridBagLayout()

    add(label("LOCAL_SECRET"), gbcLabel(0))
    add(localSecretText, gbcBody(0))

    regenLocalSecret.addActionListener {
      setState {
        state.localSecret = Encryption.generateLocalSecret()
      }
    }
    add(regenLocalSecret, gbcBody(1))

    add(label("DROPBOX_APP_KEY"), gbcLabel(2))
    add(dropboxAppKey, gbcBody(2))
    dropboxAppKey.addKeyListener(object : KeyListener {
      override fun keyTyped(e: KeyEvent?) {
      }

      override fun keyPressed(e: KeyEvent?) {
      }

      override fun keyReleased(e: KeyEvent?) {
        setState {
          state.dropboxState.appKey = dropboxAppKey.text
        }
      }
    })

    add(label("DROPBOX_REDIRECT_URI"), gbcLabel(3))
    add(dropboxRedirectUri, gbcBody(3))
    dropboxRedirectUri.addKeyListener(object : KeyListener {
      override fun keyTyped(e: KeyEvent?) {
      }

      override fun keyPressed(e: KeyEvent?) {
      }

      override fun keyReleased(e: KeyEvent?) {
        setState {
          state.dropboxState.redirectUri = dropboxRedirectUri.text
        }
      }
    })

    add(label("DROPBOX_AUTH_URI"), gbcLabel(4))
    add(dropboxAuthUri, gbcBody(4))

    add(label("DROPBOX_AUTHORIZATION_CODE"), gbcLabel(5))
    add(dropboxAccessCode, gbcBody(5))

    add(label("DROPBOX_ACCESS_TOKEN_STATUS"), gbcLabel(6))
    add(dropboxTokenStatus, gbcBody(6))

    add(label("DROPBOX_APP_ROOT_PATH"), gbcLabel(7))
    add(dropboxAppRootPath, gbcBody(7))

    add(label("MASTER_PASSWORD"), gbcLabel(8))
    masterPassword.addKeyListener(object : KeyListener {
      override fun keyTyped(e: KeyEvent?) {
      }

      override fun keyPressed(e: KeyEvent?) {
      }

      override fun keyReleased(e: KeyEvent?) {
        setState {
          this.masterPasswordOk = isPasswordOk(masterPassword.password)
          this.masterPasswordVerifyOk =
            masterPassword.password.contentEquals(masterPasswordVerify.password)
        }
      }
    })
    add(masterPassword, gbcBody(8))

    add(label("MASTER_PASSWORD_VERIFY"), gbcLabel(9))
    masterPasswordVerify.addKeyListener(object : KeyListener {
      override fun keyTyped(e: KeyEvent?) {
      }

      override fun keyPressed(e: KeyEvent?) {
      }

      override fun keyReleased(e: KeyEvent?) {
        setState {
          this.masterPasswordVerifyOk =
            masterPassword.password.contentEquals(masterPasswordVerify.password)
        }
      }
    })
    add(masterPasswordVerify, gbcBody(9))

    generateLocalInfo.addActionListener {
      // TODO dropbox access key 포맷 확인
      // TODO master password/password verify 일치 확인
      // TODO local secret과 access key 잘 보관하라는 경고 메시지 출력
      val dropboxToken = state.dropboxState.accessToken.value
      if (dropboxToken != null) {
        generateAndSaveLocalInfo(
          dropboxToken.accessToken,
          dropboxToken.refreshToken,
          masterPassword.password.concatToString()
        )
      }
    }
    add(generateLocalInfo, gbcBody(10))

    CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
      stateFlow.map { it.localSecret }.distinctUntilChanged()
        .collectLatest { localSecret ->
          localSecretText.text = "KEY: ${localSecret.localKey.toHexString()}\n" +
            "SALT: ${localSecret.passwordSalt.toHexString()}"
        }
    }

    CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
      stateFlow.map { Pair(it.dropboxState.appKey, it.dropboxState.redirectUri) }
        .distinctUntilChanged()
        .debounce(300)
        .collectLatest { (appKey, redirectUri) ->
          val url = HttpUrl.Builder()
            .scheme("https")
            .host("www.dropbox.com")
            .addPathSegments("oauth2/authorize")
            .addQueryParameter("client_id", appKey)
            .addQueryParameter("code_verifier", state.dropboxState.codeVerifier)
            .addQueryParameter("code_challenge_method", "S256")
            .addQueryParameter("code_challenge", state.dropboxState.codeChallenge)
            .addQueryParameter("redirect_uri", redirectUri)
            .addQueryParameter("response_type", "code")
            .addQueryParameter("token_access_type", "offline")
            .build()
          dropboxAuthUri.text = url.toString()
        }
    }

    CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
      dropboxAccessCode.addKeyListener(object : KeyListener {
        override fun keyTyped(e: KeyEvent?) {
        }

        override fun keyPressed(e: KeyEvent?) {
        }

        override fun keyReleased(e: KeyEvent?) {
          setState {
            state.dropboxState.accessCode = dropboxAccessCode.text
          }
        }
      })
    }

    CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
      stateFlow.map { it.dropboxState.accessCode }
        .distinctUntilChanged()
        .debounce(500)
        .collectLatest { authorizationCode ->
          /**
           * curl https://api.dropbox.com/oauth2/token \
           *   -d code=<AUTHORIZATION_CODE> \
           *   -d grant_type=authorization_code \
           *   -d redirect_uri=<REDIRECT_URI> \
           *   -d code_verifier=<VERIFICATION_CODE> \
           *   -d client_id=<APP_KEY>
           */
          setState {
            runBlocking {
              state.dropboxState.accessToken.emit(null)
            }
          }
          if (authorizationCode.isNotEmpty()) {
            val url = HttpUrl.Builder()
              .scheme("https")
              .host("api.dropbox.com")
              .addPathSegments("oauth2/token")
              .addQueryParameter("code", authorizationCode)
              .addQueryParameter("grant_type", "authorization_code")
              .addQueryParameter("redirect_uri", state.dropboxState.redirectUri)
              .addQueryParameter("code_verifier", state.dropboxState.codeVerifier)
              .addQueryParameter("client_id", state.dropboxState.appKey)
              .build()

            try {
              val response =
                okHttpClient.newCall(Request.Builder().url(url).post("".toRequestBody()).build())
                  .await()

              val responseBody = response.body!!.string()

              val token = gson.fromJson(responseBody, DropboxToken::class.java)
              setState {
                runBlocking {
                  state.dropboxState.accessToken.emit(token)
                }
              }
            } catch (e: Exception) {
              e.printStackTrace()
              runBlocking {
                state.dropboxState.accessToken.emit(null)
              }
            }
          }
        }
    }

    CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
      stateFlow.flatMapLatest { it.dropboxState.accessToken }.collectLatest { tokenStatus ->
        SwingUtilities.invokeLater {
          if (tokenStatus == null) {
            dropboxTokenStatus.text = config.getString("DROPBOX_TOKEN_NOT_READY")
            dropboxTokenStatus.background = Color.RED
          } else {
            dropboxTokenStatus.text = config.getString("DROPBOX_TOKEN_OK")
            dropboxTokenStatus.background = Color.WHITE
          }
        }
      }
    }

    CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
      stateFlow.collectLatest { state ->
        SwingUtilities.invokeLater {
          if (dropboxAppKey.text != state.dropboxState.appKey) {
            dropboxAppKey.text = state.dropboxState.appKey
          }
          if (dropboxRedirectUri.text != state.dropboxState.redirectUri) {
            dropboxRedirectUri.text = state.dropboxState.redirectUri
          }
          if (dropboxAppRootPath.text != state.dropboxState.appRootPath) {
            dropboxAppRootPath.text = state.dropboxState.appRootPath
          }
        }
      }
    }

    CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
      stateFlow.map { it.masterPasswordOk }.distinctUntilChanged().collectLatest {
        if (it) {
          masterPassword.background = Color.WHITE
        } else {
          masterPassword.isOpaque = true
          masterPassword.background = Color.RED
        }
      }
    }

    CoroutineScope(config.executors.asCoroutineDispatcher()).launch {
      stateFlow.map { it.masterPasswordVerifyOk }.distinctUntilChanged().collectLatest {
        if (it) {
          masterPasswordVerify.background = Color.WHITE
        } else {
          masterPasswordVerify.isOpaque = true
          masterPasswordVerify.background = Color.RED
        }
      }
    }
  }

  private fun isPasswordOk(password: CharArray): Boolean {
    if (password.size < 10) {
      return false
    }
    return true
  }

  private fun generateAndSaveLocalInfo(
    dropboxAccessToken: String,
    dropboxRefreshToken: String,
    masterPassword: String
  ) {
    val localInfo = LocalInfoWithRevision(
      System.currentTimeMillis(),
      LocalInfo.newBuilder()
        .setSecret(state.localSecret)
        .setStorageProfile(
          StorageProfile.newBuilder()
            .setDropbox(
              DropboxStorageProfile.newBuilder()
                .setAppName("Passzero")
                .setAppKey(state.dropboxState.appKey)
                .setAccessToken(dropboxAccessToken)
                .setRefreshToken(dropboxRefreshToken)
                .setAppRootPath(state.dropboxState.appRootPath)
            )
        )
        .build()
    )
    config.localInfoFile.writeBytes(localInfo.encode(masterPassword).toByteArray())
    initializeStorage(localInfo, masterPassword)
    appStateManager.closeSession()
  }

  private fun initializeStorage(localInfo: LocalInfoWithRevision, masterPassword: String) {
    runBlocking {
      val cryptSession = CryptSession.from(localInfo, masterPassword)
      val dropboxSession = DropboxSession(
        cryptSession,
        localInfo.localInfo.storageProfile.dropbox,
        okHttpClient
      ) { newToken ->
        // shouldn't happen
      }

      val newDirectory = dropboxSession.createDirectory("Personal")
      dropboxSession.writeConfig(
        StorageProto.Config.newBuilder().setDefaultDirectory(newDirectory.id).build()
      )
    }
  }
}

data class LocalInfoInitViewState(
  var localSecret: LocalSecret,
  var dropboxState: DropboxState,
  var masterPasswordOk: Boolean,
  var masterPasswordVerifyOk: Boolean,
)

data class DropboxState(
  var appKey: String,
  var redirectUri: String,
  var codeVerifier: String,
  var codeChallenge: String,
  var accessCode: String,
  var accessToken: MutableStateFlow<DropboxToken?>,
  var appRootPath: String,
)
