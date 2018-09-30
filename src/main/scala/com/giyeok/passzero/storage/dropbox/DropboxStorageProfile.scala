package com.giyeok.passzero.storage.dropbox

import com.giyeok.passzero.utils.ByteArrayUtil._
import com.giyeok.passzero.StorageSessionManager
import com.giyeok.passzero.storage.StorageProfile
import com.giyeok.passzero.storage.StorageProfileSpec
import com.giyeok.passzero.utils.ByteBuf
import com.giyeok.passzero.utils.ByteReader

object DropboxStorageProfile extends StorageProfileSpec {
    val name: String = "dropbox"

    val deserializer: Array[Byte] => DropboxStorageProfile = { array =>
        val reader = new ByteReader(array)

        val profileVersion = reader.readBytes(2).toSeq
        if (profileVersion != Seq(0, 1)) {
            throw new Exception("Invalid storage profile")
        }

        val appNameLength = reader.readInt()
        val appNameBytes = reader.readBytes(appNameLength)
        val appName = appNameBytes.asString

        val accessTokenLength = reader.readInt()
        val accessTokenBytes = reader.readBytes(accessTokenLength)
        val accessToken = accessTokenBytes.asString

        val rootPathLength = reader.readInt()
        val rootPathBytes = reader.readBytes(rootPathLength)
        val rootPath = rootPathBytes.asString

        new DropboxStorageProfile(appName, accessToken, rootPath)
    }
}

class DropboxStorageProfile(val appName: String, val accessToken: String, val rootPath: String) extends StorageProfile {
    val name: String = DropboxStorageProfile.name
    def infoText: String =
        s"""
           |Dropbox
           |appName=$appName
           |accessToken=$accessToken
           |rootPath=$rootPath
         """.stripMargin

    def toBytes: Array[Byte] = {
        val buf = new ByteBuf(14 + appName.length + accessToken.length + rootPath.length)

        buf.writeBytes(Seq(0, 1))

        val appNameBytes = appName.toBytes
        buf.writeInt(appNameBytes.length)
        buf.writeBytes(appNameBytes)

        val accessTokenBytes = accessToken.toBytes
        buf.writeInt(accessTokenBytes.length)
        buf.writeBytes(accessTokenBytes)

        val rootPathBytes = rootPath.toBytes
        buf.writeInt(rootPathBytes.length)
        buf.writeBytes(rootPathBytes)

        buf.finish()
    }

    def createSession(manager: StorageSessionManager): DropboxStorageSession =
        new DropboxStorageSession(this)
}
