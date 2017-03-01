package com.giyeok.passzero

import com.giyeok.passzero.utils.ByteArrayUtil._
import com.giyeok.passzero.storage.memory.MemoryStorageProfile

object Main {
    def main(args: Array[String]): Unit = {
        val localKeys = LocalKeys.generateRandomLocalInfo()
        val storageProfile = new MemoryStorageProfile

        val password = "starbucks_apple_samsung_television_coffee"

        val session = new Session(password, localKeys, storageProfile.createSession())

        val localSave = LocalInfo.save(password, session.localInfo)
        localSave.toHexMatrix foreach println

        val localSave2 = LocalInfo.save(password, session.localInfo)
        localSave2.toHexMatrix foreach println

        val restoredLocalInfo = LocalInfo.load(password, localSave)
        assert(localKeys identical restoredLocalInfo._2.localKeys)
        // TODO storageProfile도 확인

        println(s"original localKeys: ${localKeys.toReadable}")
        println(s"restored localKeys: ${restoredLocalInfo._2.localKeys.toReadable}")

        val original = "Joonsoo is real king"
        val encoded = session.encode(original.toBytes)
        println(s"encoded(${original.toBytes.length} -> ${encoded.length}):${encoded.toHexString}")
        val decoded = session.decode(encoded).asString
        println(s"decoded(${original.toBytes.length} -> ${decoded.length}):$decoded;")
        assert(original.toBytes identical decoded.toBytes)
    }
}
