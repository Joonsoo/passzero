package com.giyeok.passzero

import com.giyeok.passzero.utils.ByteArrayUtil
import org.scalatest.FlatSpec

class LocalSecretTest extends FlatSpec {

    "Every (10000 randomly generated) LocalInfo" should "successfully encoded and decoded" in {
        def test(localInfo: LocalSecret): Unit = {
            val encoded = localInfo.toAlphaDigits
            val decoded = LocalSecret.fromAlphaDigits(encoded)
            // println(s"encoded:$encoded")
            // println(s"decoded:${decoded.toAlphaDigits}")
            assert(localInfo identical decoded)
            assert(localInfo.toReadable == decoded.toReadable)
        }
        test(LocalSecret(ByteArrayUtil.fill(32, 0), ByteArrayUtil.fill(32, 0)))
        test(LocalSecret(ByteArrayUtil.fill(32, 255), ByteArrayUtil.fill(32, 255)))
        val secret = LocalSecret.generateRandomLocalInfo()
        println(secret.toAlphaDigits)
        println(secret.toReadable)
        (0 until 10000) foreach { _ =>
            test(LocalSecret.generateRandomLocalInfo())
        }
    }
}
