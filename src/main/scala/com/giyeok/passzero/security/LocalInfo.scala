package com.giyeok.passzero.security

case class LocalInfo(pwSalt: Array[Byte], localKey: Array[Byte], localIv: Array[Byte]) {
    assert(pwSalt.length == 32)
    assert(localKey.length == 32)
    assert(localIv.length == 16)
    def toReadable: String = {
        val array = pwSalt ++ localKey ++ localIv
        assert(array.length == 80)
        // 80 bytes = 640 bits
        // 5bit = 32 chars, 640 / 5 = 128
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        assert(chars.length >= 32)

        val grouped = array.grouped(5).toSeq
        // 5바이트씩 쪼개면 한 덩어리에서 8글자가 나옴
        assert(grouped forall { _.length == 5 })
        assert(grouped.length == 16)

        val rawString = (grouped flatMap { y =>
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
            indices map { chars(_) }
        }).mkString
        assert(rawString.length == 128)

        rawString.grouped(8) mkString "-"
    }

    // LocalInfo를 저장할 때는 비밀번호로 한 번 더 암호화 해서
}