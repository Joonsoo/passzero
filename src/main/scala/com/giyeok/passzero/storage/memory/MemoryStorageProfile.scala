package com.giyeok.passzero.storage.memory

import com.giyeok.passzero.storage.StorageProfile
import com.giyeok.passzero.storage.StorageProfileSpec
import com.giyeok.passzero.storage.StorageSession
import org.json4s.DefaultFormats

object MemoryStorageProfile extends StorageProfileSpec {
    implicit val formats = DefaultFormats

    val name: String = "memory"
    val deserializer: (Array[Byte]) => StorageProfile = { _ =>
        new MemoryStorageProfile()
    }
}

class MemoryStorageProfile extends StorageProfile {
    val name: String = MemoryStorageProfile.name

    lazy val toBytes: Array[Byte] = new Array[Byte](0)

    def createSession(): StorageSession =
        new MemoryStorageSession(this)
}
