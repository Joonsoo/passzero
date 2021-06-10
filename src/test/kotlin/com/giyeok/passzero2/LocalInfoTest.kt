package com.giyeok.passzero2

import com.giyeok.passzero2.core.LocalInfo
import com.giyeok.passzero2.core.LocalSecret
import com.giyeok.passzero2.core.SessionProfile
import com.giyeok.passzero2.sessions.DropboxProfile
import java.util.*
import kotlin.test.Test

class LocalInfoTest {
    private fun byteArray(length: Int, value: Int): ByteArray {
        val array = ByteArray(length)
        for (i in 0 until length) {
            array[i] = value.toByte()
        }
        return array
    }

    @Test
    fun testLocalSecretEncodeAndDecode() {
        fun test(secret: LocalSecret) {
            assert(secret.contentEquals(secret))

            val base64 = LocalSecret.fromBase64(secret.toBase64())
            assert(secret.contentEquals(base64))
        }

        test(LocalSecret(byteArray(32, 0), byteArray(32, 0)))
        test(LocalSecret(byteArray(32, 255), byteArray(32, 255)))
        for (i in 1..1000) {
            test(LocalSecret.generateRandomLocalInfo())
        }
    }

    fun Random.storageProfile(): SessionProfile =
        DropboxProfile(this.string(10), this.string(10), this.string(10))

    fun Random.bytes(length: Int): ByteArray {
        val array = ByteArray(length)
        for (i in 0 until length) {
            array[i] = this.nextInt(256).toByte()
        }
        return array
    }

    fun Random.string(length: Int): String {
        val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val builder = StringBuilder()
        for (i in 0 until length) {
            builder.append(chars[this.nextInt(chars.size)])
        }
        return builder.toString()
    }

    fun Random.localInfo(): LocalInfo =
            LocalInfo(this.nextLong(), this.nextLong(),
                    LocalSecret.generateRandomLocalInfo(),
                    this.storageProfile())

    @Test
    fun testLocalInfoEncodeAndDecode() {
        fun test(localInfo: LocalInfo, password: String) {
            assert(localInfo contentEquals localInfo)

            val restored = LocalInfo.decode(password, localInfo.encode(password))
            assert(localInfo contentEquals restored)
        }

        val r = Random()
        test(r.localInfo(), "")
        test(LocalInfo(0, 0, LocalSecret(byteArray(32, 0), byteArray(32, 0)), r.storageProfile()), r.string(15))
        test(LocalInfo(Long.MAX_VALUE, Long.MAX_VALUE, LocalSecret(byteArray(32, 255), byteArray(32, 255)), r.storageProfile()), r.string(15))
        for (i in 1..10) {
            test(r.localInfo(), r.string(15))
        }
    }
}
