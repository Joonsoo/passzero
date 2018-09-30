package com.giyeok.passzero2.core

import java.io.File
import java.time.Duration

class Session(private val rootName: String, private val secretKey: ByteArray, private val storageSession: StorageSession) {
    companion object {
        private fun xor(a: ByteArray, b: ByteArray): ByteArray {
            assert(a.size == b.size)
            val newArray = ByteArray(a.size)
            for (i in 0 until a.size) {
                newArray[i] = (a[i].toInt() xor b[i].toInt()).toByte()
            }
            return newArray
        }

        fun calculateSecretKey(localInfo: LocalInfo, password: String): ByteArray {
            val pwHash = Crypto.PasswordHash.generateHash(localInfo.localSecret.pwSalt, password)
            return xor(localInfo.localSecret.localKey,
                    xor(pwHash.sliceArray(0 until 32), pwHash.sliceArray(32 until 64)))
        }

        fun load(passwordText: String, localInfoFile: File): Session {
            val localInfo = LocalInfo.load(passwordText, localInfoFile)

            if (Duration.ofMillis(System.currentTimeMillis() - localInfo.timestamp) > Duration.ofDays(60)) {
                // TODO update LocalInfo File
            }

            return Session(
                    "${localInfo.revision}",
                    calculateSecretKey(localInfo, passwordText),
                    localInfo.storageProfile.createSession())
        }
    }
}
