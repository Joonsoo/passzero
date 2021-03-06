package com.giyeok.passzero

import com.giyeok.passzero.storage.{Entity, Path}
import com.giyeok.passzero.storage.memory.MemoryStorageProfile
import com.giyeok.passzero.utils.ByteArrayUtil._

import scala.concurrent.ExecutionContext

object Main {
    def main(args: Array[String]): Unit = {
        implicit val ec = ExecutionContext.global
        val localKeys = LocalSecret.generateRandomLocalInfo()

        val password = "starbucks_apple_samsung_television_coffee"

        val storageProfile = new MemoryStorageProfile
        val session = new Session(0, password, localKeys, storageProfile)

        val localSave = LocalInfo.save(password, session.localInfo)
        localSave.printHexMatrix()

        val localSave2 = LocalInfo.save(password, session.localInfo)
        localSave2.printHexMatrix()

        val restoredLocalInfo = LocalInfo.load(password, localSave)
        assert(localKeys identical restoredLocalInfo._2.localSecret)
        // TODO storageProfile도 확인

        println(s"original localKeys: ${localKeys.toReadable}")
        println(s"restored localKeys: ${restoredLocalInfo._2.localSecret.toReadable}")

        val original = "Joonsoo is real king"
        val (initVec, encoded) = session.encode(original.toBytes)
        println(s"encoded(${original.toBytes.length} -> ${encoded.length}):${encoded.toHexString}")
        val decoded = session.decode(encoded, initVec).asString
        println(s"decoded(${original.toBytes.length} -> ${decoded.length}):$decoded;")
        assert(original.toBytes identical decoded.toBytes)

        (1 to 100) foreach { iteration =>
            println(s"  * iteration $iteration")
            val path = Path("")
            session.putString(path, original)
            storageProfile.session.printHexMatrixOfFile(path)
            for {
                Some(Entity(decoded)) <- session.getAsString(path)
            } yield {
                println(s"decoded: $decoded")
                assert(original == decoded)
            }
        }
    }
}
