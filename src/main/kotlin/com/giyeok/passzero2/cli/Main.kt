package com.giyeok.passzero2.cli

import com.giyeok.passzero2.core.CryptSession
import com.giyeok.passzero2.core.LocalInfoWithRevision
import com.giyeok.passzero2.core.storage.DropboxSession
import com.google.protobuf.ByteString
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.io.File
import kotlin.system.exitProcess

object Main {
  @JvmStatic
  fun main(args: Array<String>) = runBlocking {
    val parser = ArgParser("passzero")
    val localInfoPath by parser.option(ArgType.String, "localInfo", "l", "Path to local info")
      .default("./localInfo.p0")
    val argMasterPassword by parser.option(ArgType.String, "password", "p", "Master Password")

    parser.parse(args)

    val masterPassword: String = if (argMasterPassword != null) argMasterPassword!! else {
      print("Input Master Password: ")
      val console = System.console()
      console?.readPassword()?.concatToString() ?: readLine()!!
    }

    val localInfo = LocalInfoWithRevision.decode(
      masterPassword,
      ByteString.readFrom(File(localInfoPath).inputStream())
    )
    val cryptSession = CryptSession.from(localInfo.localInfoWithRevision, masterPassword)
    val okHttpClient = OkHttpClient()
    val dropboxSession = DropboxSession(
      cryptSession,
      localInfo.localInfoWithRevision.localInfo.storageProfile.dropbox,
      okHttpClient
    ) { newToken ->
      // TODO update localInfo
    }

    val config = dropboxSession.getConfig()

    val directoryList = dropboxSession.getDirectoryList()
    println("Directories:")
    directoryList.forEach { println(it) }

    var currentDirectory = config.defaultDirectory

    val entryList0 = dropboxSession.getEntryListCache(currentDirectory)
    if (entryList0 == null) {
      println("Loading all entries of $currentDirectory...")
    }
    val entryList =
      (entryList0
        ?: dropboxSession.createEntryListCache(currentDirectory)).entriesList.sortedBy { it.info.name }

    while (true) {
      print("> ")
      val command = readLine()
      val tokens = command?.split(' ')

      when (tokens?.get(0)) {
        "list" -> {
          entryList.forEachIndexed { index, entry ->
            println("$index ${entry.info.name}")
          }
        }
        "get" -> {
          val idx = tokens[1].toInt()
          val entry = entryList[idx]
          println("Directory: ${entry.directory}")
          println("ID: ${entry.id}")
          println("Type: ${entry.info.type}")
          println("Name: ${entry.info.name}")
          val entryDetail = dropboxSession.getEntryDetail(entry.directory, entry.id)
          println(entryDetail)
        }
        "q", "quit", "exit" -> {
          println("Bye!")
          exitProcess(0)
        }
        else -> {
          println("list: list all sheets")
        }
      }
    }
  }
}
