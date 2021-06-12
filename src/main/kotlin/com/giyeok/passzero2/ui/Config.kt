package com.giyeok.passzero2.ui

class Config {
  fun getString(stringKey: String): String = when (stringKey) {
    "app_title" -> "Passzero"
    else -> stringKey
  }
}