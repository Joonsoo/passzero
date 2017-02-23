package com.giyeok.passzero.security

import com.giyeok.passzero.ByteArrayUtil._

object LocalInfo {
    val encodingChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    assert(encodingChars.length >= 32)

    def fromAlphaDigits(string: String): LocalInfo = {
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
        assert(totalBytes.length == 80)
        LocalInfo(totalBytes.slice(0, 32).toArray, totalBytes.slice(32, 64).toArray, totalBytes.slice(64, 80).toArray)
    }
}

// TODO pwSalt과 localIv의 절반은 remote에 저장해서 LocalInfo는 40바이트로 맞춘다 -> 인코딩했을 때 너무 길어서 불편
case class LocalInfo(pwSalt: Array[Byte], localKey: Array[Byte], localIv: Array[Byte]) {
    assert(pwSalt.length == 32)
    assert(localKey.length == 32)
    assert(localIv.length == 16)
    def toAlphaDigits: String = {
        val array = pwSalt ++ localKey ++ localIv
        assert(array.length == 80)
        // 80 bytes = 640 bits
        // 5bit = 32 chars, 640 / 5 = 128

        val grouped = array.grouped(5).toSeq
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
            indices map { LocalInfo.encodingChars(_) }
        }).mkString
        assert(string.length == 128)
        string
    }

    def toReadable: String =
        toAlphaDigits.grouped(8) mkString "-"

    def identical(other: LocalInfo): Boolean =
        (pwSalt identical other.pwSalt) && (localKey identical other.localKey) && (localIv identical other.localIv)

    // LocalInfo를 저장할 때는 비밀번호로 한 번 더 암호화 해서
}
