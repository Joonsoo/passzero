package com.giyeok.passzero2.core.sessions

import com.giyeok.passzero2.core.StorageProfile
import com.giyeok.passzero2.core.StorageSession
import com.giyeok.passzero2.util.readBytesFully
import java.io.ByteArrayInputStream
import java.io.DataInputStream

class DropboxProfile(val appName: String, val accessToken: String, val rootPath: String) : StorageProfile {
    companion object : StorageProfile.Loader {
        const val name = "dropbox"

        override fun fromBytes(bytes: ByteArray): DropboxProfile {
            val stream = DataInputStream(ByteArrayInputStream(bytes))

            val profileVersion = stream.readBytesFully(2)
            if (!profileVersion.contentEquals(byteArrayOf(0, 1))) {
                throw Exception("Unsupported dropbox version: $profileVersion")
            }


            val appNameLength = stream.readInt()
            val appName = String(stream.readBytes(appNameLength))

            val accessTokenLength = stream.readInt()
            val accessToken = String(stream.readBytes(accessTokenLength))

            val rootPathLength = stream.readInt()
            val rootPath = String(stream.readBytes(rootPathLength))

            return DropboxProfile(appName, accessToken, rootPath)
        }
    }

    override fun createSession(): DropboxSession = DropboxSession(this)
}

class DropboxSession(private val profile: DropboxProfile) : StorageSession {

}
