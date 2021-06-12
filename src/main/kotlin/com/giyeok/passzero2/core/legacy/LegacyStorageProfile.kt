package com.giyeok.passzero2.core.legacy

import com.giyeok.passzero2.core.*
import com.google.protobuf.ByteString

object LegacyStorageProfile {
  fun fromBytes(bytes: ByteString): LocalInfoProto.StorageProfile {
    val splitAt = bytes.indexOfFirst { it.toInt() == 0 }
    check(splitAt >= 0)
    when (val storageType = bytes.substring(0, splitAt).toStringUtf8()) {
      "dropbox" -> {
        val reader = bytes.substring(splitAt + 1).toInputStream()
        check(reader.readShort().toInt() == 1)
        val appNameLength = reader.readInt()
        val appName = reader.readBytes(appNameLength).toStringUtf8()

        val accessTokenLength = reader.readInt()
        val accessToken = reader.readBytes(accessTokenLength).toStringUtf8()

        val rootPathLength = reader.readInt()
        val rootPath = reader.readBytes(rootPathLength).toStringUtf8()

        return LocalInfoProto.StorageProfile.newBuilder().setDropbox(
          LocalInfoProto.DropboxStorageProfile.newBuilder()
            .setAppName(appName)
            .setAccessToken(accessToken)
            .setAppRootPath(rootPath)
        ).build()
      }
      else -> throw IllegalArgumentException("Unsupported storageType: $storageType")
    }
  }
}
