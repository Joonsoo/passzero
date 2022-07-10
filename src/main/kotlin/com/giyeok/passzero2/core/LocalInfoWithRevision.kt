package com.giyeok.passzero2.core

import com.giyeok.passzero2.core.legacy.LegacyLocalSecret
import com.giyeok.passzero2.core.legacy.LegacyStorageProfile
import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import java.io.ByteArrayOutputStream

data class LocalInfoWithRevision(
  val revision: Long,
  val localInfo: LocalInfoProto.LocalInfo
) {
  data class LocalInfoWithTimestamp(
    val localInfoWithRevision: LocalInfoWithRevision,
    val timestamp: Timestamp
  )

  companion object {
    fun decode(password: String, input: ByteString): LocalInfoWithTimestamp {
      val reader = input.toInputStream()
      check(reader.readBytes(4) == ByteString.copyFromUtf8("GYPZ"))
      val versionNum = reader.readShort().toInt()
      check(versionNum == 1 || versionNum == 2)

      val timestamp = Timestamps.fromMillis(reader.readLong())
      val revision = reader.readLong()
      val localInfoSalt = Encryption.PasswordSalt(reader.readBytes(32))
      val localInfoIv =
        Encryption.InitVector(reader.readBytes(Encryption.InitVector.INITVEC_LENGTH))
      val localInfoKey =
        Encryption.PasswordHasher.generateHash(localInfoSalt, password).bytes.substring(0, 32)

      val encodedContent = reader.readRest()
      val content = Encryption.AES256CBC.decode(encodedContent, localInfoKey, localInfoIv)

      return when (versionNum) {
        1 -> {
          val localSecret = LegacyLocalSecret.fromBytes(content.substring(0, 64))
          val storageProfile = LegacyStorageProfile.fromBytes(content.substring(64))
          val localInfo =
            LocalInfoProto.LocalInfo.newBuilder().setSecret(localSecret)
              .setStorageProfile(storageProfile).build()
          LocalInfoWithTimestamp(LocalInfoWithRevision(revision, localInfo), timestamp)
        }
        2 -> {
          // Use protobuf LocalSecret and StorageProfile
          val localInfo = LocalInfoProto.LocalInfo.parseFrom(content)
          LocalInfoWithTimestamp(LocalInfoWithRevision(revision, localInfo), timestamp)
        }
        else -> throw IllegalArgumentException("Unsupported version number: $versionNum")
      }
    }
  }

  fun encode(password: String, timestamp: Timestamp): ByteString {
    val buf = ByteArrayOutputStream()

    buf.writeBytes(ByteString.copyFromUtf8("GYPZ"))
    buf.write(0)
    buf.write(2)
    buf.writeLong(Timestamps.toMillis(timestamp))
    buf.writeLong(revision)

    val passwordPair = Encryption.PasswordHasher.generateHashAndSalt(password)
    buf.writeBytes(passwordPair.salt.bytes)

    val iv = Encryption.InitVector.generate()
    buf.writeBytes(iv.bytes)

    val localInfoKey = passwordPair.hash.bytes.substring(0, 32)

    val encodedContent = Encryption.AES256CBC.encode(localInfo.toByteString(), localInfoKey, iv)
    buf.writeBytes(encodedContent)

    return ByteString.copyFrom(buf.toByteArray())
  }

  fun encode(password: String): ByteString =
    encode(password, Timestamps.fromMillis(System.currentTimeMillis()))
}
