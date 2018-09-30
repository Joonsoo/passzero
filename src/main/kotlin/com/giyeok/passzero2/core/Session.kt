package com.giyeok.passzero2.core

import java.io.File
import java.time.Duration

class Session(private val rootName: String, private val secretKey: ByteArray, private val storageSession: StorageSession) {
    companion object {
        fun load(passwordText: String, localInfoFile: File): Session {
            val localInfo = LocalInfo.load(passwordText, localInfoFile)

            if (Duration.ofMillis(System.currentTimeMillis() - localInfo.timestamp) > Duration.ofDays(60)) {
                // TODO update LocalInfo File
            }

            return Session(
                    "${localInfo.revision}",
                    localInfo.secretKey,
                    localInfo.storageProfile.createSession())
        }
    }
}
