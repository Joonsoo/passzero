package com.giyeok.passzero2.gui

import kotlinx.coroutines.CoroutineScope
import java.awt.Font
import java.io.File
import java.util.concurrent.ExecutorService
import kotlin.coroutines.EmptyCoroutineContext

class Config(
  val localInfoFile: File = File("./localInfo.p0"),
  val executors: ExecutorService
) {
  val defaultFont = Font(Font.MONOSPACED, 0, 15)
  val bigFont = Font(Font.MONOSPACED, 0, 24)

  fun getString(stringKey: String): String = when (stringKey) {
    "app_title" -> "Passzero"
    "ENTRY_TYPE_LOGIN" -> "Login"
    "ENTRY_TYPE_NOTE" -> "Note"
    "ENTRY_DETAIL_ITEM_USERNAME" -> "Username"
    "ENTRY_DETAIL_ITEM_PASSWORD" -> "Password"
    "ENTRY_DETAIL_ITEM_WEBSITE" -> "Website"
    "ENTRY_DETAIL_ITEM_NOTE" -> "Note"
    "ENTRY_EDIT" -> "Edit this entry"
    "ENTRY_DELETE" -> "Delete this entry"
    "ENTRY_CREATE_SAVE" -> "Create"
    "ENTRY_CREATE_CANCEL" -> "Cancel"
    "ENTRY_EDIT_SAVE" -> "Save"
    "ENTRY_EDIT_CANCEL" -> "Cancel"
    "SELECT_ENTRY" -> "Select an entry"
    "NEW_ENTRY" -> "New"
    else -> stringKey
  }
}