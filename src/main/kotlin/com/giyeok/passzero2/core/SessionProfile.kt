package com.giyeok.passzero2.core

import com.giyeok.passzero2.sessions.DropboxProfile

interface SessionProfile {
    companion object {
        val supportedStorages: List<SessionProfile.Loader> = listOf(
                DropboxProfile
        )

        val supportedStoragesMap: Map<String, (ByteArray) -> SessionProfile> = supportedStorages.map { s ->
            s.name to s::fromBytes
        }.toMap()

        fun fromBytes(bytes: ByteArray): SessionProfile {
            val separateAt = bytes.indexOf(0)
            val storageType = String(bytes.sliceArray(0 until separateAt))
            val profileInfo = bytes.sliceArray((separateAt + 1) until bytes.size)

            val deserializer = supportedStoragesMap[storageType] ?: throw Exception("Unknown deserializer")
            return deserializer(profileInfo)
        }
    }

    interface Loader {
        val name: String
        fun fromBytes(bytes: ByteArray): SessionProfile
    }

    val loader: Loader

    fun createSession(root: String, secretKey: ByteArray): Session

    infix fun contentEquals(other: SessionProfile): Boolean

    fun toBytes(): ByteArray
}
