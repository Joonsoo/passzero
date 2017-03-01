package com.giyeok.passzero

import com.giyeok.passzero.ByteArrayUtil._

object LocalKeys {
    val encodingChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    assert(encodingChars.length >= 32)

    def fromBytes(bytes: Seq[Byte]): LocalKeys = {
        assert(bytes.length == 80)
        LocalKeys(bytes.slice(0, 32).toArray, bytes.slice(32, 64).toArray, bytes.slice(64, 80).toArray)
    }

    def fromAlphaDigits(string: String): LocalKeys = {
        assert(string.length == 128)
        // 8글자에서 5바이트씩 나옴
        val totalBytes: Seq[Byte] = (string.grouped(8) flatMap { g =>
            val idx = g.toCharArray map { encodingChars.indexOf(_) }
            assert(idx forall { x => x < 32 })
            Seq(
                idx(0) << 3 | (idx(1) >> 2),
                ((idx(1) & 0x3) << 6) | (idx(2) << 1) | (idx(3) >> 4),
                ((idx(3) & 0xf) << 4) | (idx(4) >> 1),
                ((idx(4) & 0x1) << 7) | (idx(5) << 2) | (idx(6) >> 3),
                ((idx(6) & 0x7) << 5) | idx(7)
            )
        }).toSeq map { _.toByte }
        fromBytes(totalBytes)
    }

    def generateRandomLocalInfo(): LocalKeys = {
        import Security.secureRandom
        LocalKeys(secureRandom(32), secureRandom(32), secureRandom(16))
    }
}

case class LocalKeys(pwSalt: Array[Byte], localKey: Array[Byte], localIv: Array[Byte]) {
    assert(pwSalt.length == 32)
    assert(localKey.length == 32)
    assert(localIv.length == 16)

    def toBytes: Array[Byte] = {
        val array = pwSalt ++ localKey ++ localIv
        array ensuring array.length == 80
    }

    def toAlphaDigits: String = {
        // 80 bytes = 640 bits
        // 5bit = 32 chars, 640 / 5 = 128

        val grouped = this.toBytes.grouped(5).toSeq
        // 5바이트씩 쪼개면 한 덩어리에서 8글자가 나옴
        assert(grouped forall { _.length == 5 })
        assert(grouped.length == 16)

        val string = (grouped flatMap { y =>
            val x = y map { _.toInt & 0xff }
            val indices = Seq(
                x(0) >> 3,
                ((x(0) & 0x7) << 2) | (x(1) >> 6),
                (x(1) >> 1) & 0x1f,
                ((x(1) & 0x1) << 4) | (x(2) >> 4),
                ((x(2) & 0xf) << 1) | (x(3) >> 7),
                (x(3) >> 2) & 0x1f,
                ((x(3) & 0x3) << 3) | (x(4) >> 5),
                x(4) & 0x1f
            )
            // println(indices)
            indices map { LocalKeys.encodingChars(_) }
        }).mkString
        assert(string.length == 128)
        string
    }

    def toReadable: String =
        toAlphaDigits.grouped(8) mkString "-"

    def identical(other: LocalKeys): Boolean =
        (pwSalt identical other.pwSalt) && (localKey identical other.localKey) && (localIv identical other.localIv)

    // LocalInfo를 저장할 때는 비밀번호+AES256EBC로 암호화 해서 저장
}
