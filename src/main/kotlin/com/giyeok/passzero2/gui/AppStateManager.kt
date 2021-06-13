package com.giyeok.passzero2.gui

import com.giyeok.passzero2.core.storage.StorageSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking

class AppStateManager(private val config: Config) {
  sealed class AppStateEvent

  object LocalInfoNotExists : AppStateEvent()
  object PasswordNotReady : AppStateEvent()
  data class SessionReady(val session: StorageSession) : AppStateEvent()

  private val _state = MutableStateFlow(if (config.localInfoFile.exists()) PasswordNotReady else LocalInfoNotExists)
  val state = _state.asSharedFlow()

  fun sessionReady(session: StorageSession) {
    runBlocking {
      _state.emit(SessionReady(session))
    }
  }

  fun closeSession() {
    runBlocking {
      _state.emit(PasswordNotReady)
    }
  }
}
