package com.giyeok.passzero.ui

import java.io.File

case class Config(stringRegistry: StringRegistry, localInfoFile: File) {
  def getString(string: String): String = stringRegistry.get(string)
}
