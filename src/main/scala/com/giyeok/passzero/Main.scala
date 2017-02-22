package com.giyeok.passzero

import com.giyeok.passzero.ByteArrayUtil.Implicit
import com.giyeok.passzero.security.Security._

object Main {
    def main(args: Array[String]): Unit = {
        val localInfo = generateRandomLocalInfo()
        val password = "hello 123"

        println(s"localInfo:${localInfo.toReadable}")

        val original = "Joonsoo is real king"
        val encoded = encodeText(password, localInfo, original)
        println(s"encoded(${original.getBytes("UTF-8").length} -> ${encoded.length}):${encoded.toHexString}")
        val decoded = decodeText(password, localInfo, encoded)
        println(s"decoded:$decoded;")
    }
}
