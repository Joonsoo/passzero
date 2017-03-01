package com.giyeok.passzero.storage.memory

import com.giyeok.passzero.storage.StorageProfile
import com.giyeok.passzero.storage.StorageProfileSpec
import com.giyeok.passzero.storage.StorageSession
import com.giyeok.passzero.utils.ByteArrayUtil._
import org.json4s.DefaultFormats

object MemoryStorageProfile extends StorageProfileSpec {
    private val specString = "memory storage does not have storage profile info"
    implicit val formats = DefaultFormats

    val name: String = "memory"
    val deserializer: (Array[Byte]) => StorageProfile = { array =>
        new MemoryStorageProfile() ensuring (array.asString == specString)
    }
}

class MemoryStorageProfile extends StorageProfile {
    val name: String = MemoryStorageProfile.name

    lazy val toBytes: Array[Byte] = MemoryStorageProfile.specString.toBytes

    def createSession(): StorageSession =
        new MemoryStorageSession(this)
}
