package com.giyeok.passzero.storage.memory

import com.giyeok.passzero.StorageSessionManager
import com.giyeok.passzero.storage.StorageProfile
import com.giyeok.passzero.storage.StorageProfileSpec
import com.giyeok.passzero.utils.ByteArrayUtil._
import org.json4s.DefaultFormats

object MemoryStorageProfile extends StorageProfileSpec {
    private val specString = "memory storage does not have storage profile info"
    implicit val formats = DefaultFormats

    val name: String = "memory"
    val deserializer: (Array[Byte]) => MemoryStorageProfile = { array =>
        new MemoryStorageProfile() ensuring (array.asString == specString)
    }
}

class MemoryStorageProfile extends StorageProfile {
    val name: String = MemoryStorageProfile.name
    def infoText: String = "Memory Storage"

    lazy val toBytes: Array[Byte] = MemoryStorageProfile.specString.toBytes

    def createSession(manager: StorageSessionManager): MemoryStorageSession =
        new MemoryStorageSession(this)
}
