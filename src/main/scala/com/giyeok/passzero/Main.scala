package com.giyeok.passzero

import com.giyeok.passzero.ByteArrayUtil.Implicit
import com.giyeok.passzero.Security._

object Main {
    def main(args: Array[String]): Unit = {
        val localKeys = LocalKeys.generateRandomLocalInfo()
        val password = "hello 123"

        println(s"localInfo:${localKeys.toReadable}")

        val original = "Joonsoo is real king"
        val encoded = encodeText(password, localKeys, original)
        println(s"encoded(${original.getBytes("UTF-8").length} -> ${encoded.length}):${encoded.toHexString}")
        val decoded = decodeText(password, localKeys, encoded)
        println(s"decoded:$decoded;")
    }
}
