package com.giyeok.passzero

import com.giyeok.passzero.utils.ByteArrayUtil
import org.scalatest.FlatSpec

class LocalKeysTest extends FlatSpec {

    "Every (10000 randomly generated) LocalInfo" should "successfully encoded and decoded" in {
        def test(localInfo: LocalKeys): Unit = {
            val encoded = localInfo.toAlphaDigits
            val decoded = LocalKeys.fromAlphaDigits(encoded)
            // println(s"encoded:$encoded")
            // println(s"decoded:${decoded.toAlphaDigits}")
            assert(localInfo identical decoded)
            assert(localInfo.toReadable == decoded.toReadable)
        }
        test(LocalKeys(ByteArrayUtil.fill(32, 0), ByteArrayUtil.fill(32, 0), ByteArrayUtil.fill(16, 0)))
        test(LocalKeys(ByteArrayUtil.fill(32, 255), ByteArrayUtil.fill(32, 255), ByteArrayUtil.fill(16, 255)))
        (0 until 10000) foreach { _ =>
            test(LocalKeys.generateRandomLocalInfo())
        }
    }
}
