package com.giyeok.passzero

import com.giyeok.passzero.security.LocalInfo
import com.giyeok.passzero.security.Security
import org.scalatest.FlatSpec

class LocalInfoTest extends FlatSpec {

    "Every (100000 randomly generated) LocalInfo" should "successfully encoded and decoded" in {
        def test(localInfo: LocalInfo): Unit = {
            val encoded = localInfo.toAlphaDigits
            val decoded = LocalInfo.fromAlphaDigits(encoded)
            // println(s"encoded:$encoded")
            // println(s"decoded:${decoded.toAlphaDigits}")
            assert(localInfo identical decoded)
            assert(localInfo.toReadable == decoded.toReadable)
        }
        test(LocalInfo(ByteArrayUtil.fill(32, 0), ByteArrayUtil.fill(32, 0), ByteArrayUtil.fill(16, 0)))
        test(LocalInfo(ByteArrayUtil.fill(32, 255), ByteArrayUtil.fill(32, 255), ByteArrayUtil.fill(16, 255)))
        (0 until 100000) foreach { _ =>
            test(Security.generateRandomLocalInfo())
        }
    }
}
