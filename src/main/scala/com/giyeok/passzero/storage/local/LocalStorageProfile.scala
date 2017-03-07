package com.giyeok.passzero.storage.local

import java.io.File
import com.giyeok.passzero.StorageSessionManager
import com.giyeok.passzero.storage.StorageProfile
import com.giyeok.passzero.storage.StorageProfileSpec
import com.giyeok.passzero.utils.ByteArrayUtil._

object LocalStorageProfile extends StorageProfileSpec {
    val name: String = "local"

    val deserializer: (Array[Byte]) => LocalStorageProfile = { array =>
        val rootDirectory = new File(array.asString)
        if (!rootDirectory.exists()) {
            throw new Exception(s"LocalStorage root does not exist! ${rootDirectory.getCanonicalPath}")
        }
        if (!rootDirectory.isDirectory) {
            throw new Exception("LocalStorage root must be a directory! ${rootDirectory.getCanonicalPath}")
        }
        new LocalStorageProfile(rootDirectory)
    }
}

class LocalStorageProfile(rootDirectory: File) extends StorageProfile {
    val name: String = LocalStorageProfile.name
    def infoText: String =
        s"""
            |Local Storage
            |root=${rootDirectory.getCanonicalPath}
          """.stripMargin.trim

    def toBytes: Array[Byte] = rootDirectory.getCanonicalPath.toBytes

    def createSession(manager: StorageSessionManager): LocalStorageSession =
        new LocalStorageSession(this, rootDirectory, manager)
}
