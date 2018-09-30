package com.giyeok.passzero2.core.storages

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.giyeok.passzero2.core.StorageProfile
import com.giyeok.passzero2.core.StorageSession
import com.giyeok.passzero2.util.readBytesFully
import com.giyeok.passzero2.util.toHexString
import java.io.*

class DropboxProfile(val appName: String, val accessToken: String, val rootPath: String) : StorageProfile {

    companion object : StorageProfile.Loader {
        override val name = "dropbox"

        override fun fromBytes(bytes: ByteArray): DropboxProfile {
            val stream = DataInputStream(ByteArrayInputStream(bytes))

            val profileVersion = stream.readBytesFully(2)
            if (!profileVersion.contentEquals(byteArrayOf(0, 1))) {
                throw Exception("Unsupported dropbox version: ${profileVersion.toHexString()}, ${bytes.toHexString()}")
            }

            try {
                val appNameLength = stream.readInt()
                val appName = String(stream.readBytesFully(appNameLength))

                val accessTokenLength = stream.readInt()
                val accessToken = String(stream.readBytesFully(accessTokenLength))

                val rootPathLength = stream.readInt()
                val rootPath = String(stream.readBytesFully(rootPathLength))

                return DropboxProfile(appName, accessToken, rootPath)
            } catch (eof: EOFException) {
                throw Exception("EOF Exception: ${bytes.toHexString()}")
            }
        }
    }

    override val spec: StorageProfile.Loader = Companion

    override fun createSession(): DropboxSession = DropboxSession(this)

    override fun contentEquals(other: StorageProfile): Boolean =
            when (other) {
                is DropboxProfile ->
                    this.appName == other.appName &&
                            this.accessToken == other.accessToken &&
                            this.rootPath == other.rootPath
                else -> false
            }

    override fun toBytes(): ByteArray {
        val arrayStream = ByteArrayOutputStream()
        val stream = DataOutputStream(arrayStream)

        stream.write(0)
        stream.write(1)

        val appNameBytes = appName.toByteArray()
        stream.writeInt(appNameBytes.size)
        stream.write(appNameBytes)

        val accessTokenBytes = accessToken.toByteArray()
        stream.writeInt(accessTokenBytes.size)
        stream.write(accessTokenBytes)

        val rootPathBytes = rootPath.toByteArray()
        stream.writeInt(rootPathBytes.size)
        stream.write(rootPathBytes)

        return arrayStream.toByteArray()
    }
}

class DropboxSession(private val profile: DropboxProfile) : StorageSession {
    private val client = DbxClientV2(DbxRequestConfig.newBuilder(profile.appName).build(), profile.accessToken)

    fun firstInfo() {
        // TODO 처음 접속해서 빠르게 받아올 수 있는 초기화 정보
        // 현재 선택된 디렉토리, 캐시된 정보들
    }

    fun directories() {

    }

    fun sheetsList() {

    }

    fun clearCaches() {

    }

    fun sheetInfo(directoryId: String, sheetId: String) {

    }

    fun updateSheet(directoryId: String, sheetId: String, newSheetInfo: String) {

    }

    fun deleteSheet(directoryId: String, sheetId: String) {

    }

    fun newSheet(directoryId: String, sheetId: String, newSheetInfo: String) {

    }

    fun newDirectory() {

    }

    fun deleteDirectory() {

    }

    fun dumpTo(): String {
        // 아마도 json으로?
        TODO()
    }

    fun importFrom(dumped: String) {
        // TODO merge policy
    }
}
