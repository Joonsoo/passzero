package com.giyeok.passzero2.core

import com.giyeok.passzero2.core.sessions.DropboxProfile

interface StorageProfile {
    companion object {
        val supportedStorages: List<StorageProfile.Loader> = listOf(
                DropboxProfile
        )

        val supportedStoragesMap: Map<String, (ByteArray) -> StorageProfile> = supportedStorages.map { s ->
            s.name to s::fromBytes
        }.toMap()

        fun fromBytes(bytes: ByteArray): StorageProfile {
            val separate = bytes.indexOf(0)
            val storageType = String(bytes.sliceArray(0 until separate))
            val profileInfo = bytes.sliceArray(separate until bytes.size)

            val deserializer = supportedStoragesMap[storageType] ?: throw Exception("Unknown deserializer")
            return deserializer(profileInfo)
        }
    }

    interface Loader {
        val name: String
        fun fromBytes(bytes: ByteArray): StorageProfile
    }

    fun createSession(): StorageSession
}

interface StorageSession {

}