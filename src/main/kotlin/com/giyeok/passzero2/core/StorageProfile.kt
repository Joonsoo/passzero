package com.giyeok.passzero2.core

import com.giyeok.passzero2.core.storages.DropboxProfile

interface StorageProfile {
    companion object {
        val supportedStorages: List<StorageProfile.Loader> = listOf(
                DropboxProfile
        )

        val supportedStoragesMap: Map<String, (ByteArray) -> StorageProfile> = supportedStorages.map { s ->
            s.name to s::fromBytes
        }.toMap()

        fun fromBytes(bytes: ByteArray): StorageProfile {
            val separateAt = bytes.indexOf(0)
            val storageType = String(bytes.sliceArray(0 until separateAt))
            val profileInfo = bytes.sliceArray((separateAt + 1) until bytes.size)

            val deserializer = supportedStoragesMap[storageType] ?: throw Exception("Unknown deserializer")
            return deserializer(profileInfo)
        }
    }

    interface Loader {
        val name: String
        fun fromBytes(bytes: ByteArray): StorageProfile
    }

    val spec: Loader

    fun createSession(): StorageSession

    fun contentEquals(other: StorageProfile): Boolean

    fun toBytes(): ByteArray
}

interface StorageSession {

}