package com.giyeok.passzero

import com.giyeok.passzero.ByteArrayUtil._
import com.giyeok.passzero.storage.memory.MemoryStorageProfile

object Main {
    def main(args: Array[String]): Unit = {
        val localKeys = LocalKeys.generateRandomLocalInfo()
        val storageProfile = new MemoryStorageProfile

        val password = "starbucks"

        val session = new Session(password, localKeys, storageProfile.createSession())

        println(s"localKeys:${localKeys.toReadable}")

        val original = "Joonsoo is real king"
        val encoded = session.encode(original.toBytes)
        println(s"encoded(${original.toBytes.length} -> ${encoded.length}):${encoded.toHexString}")
        val decoded = session.decode(encoded).asString
        println(s"decoded(${original.toBytes.length} -> ${decoded.length}):$decoded;")
        assert(original.toBytes identical decoded.toBytes)
    }
}
