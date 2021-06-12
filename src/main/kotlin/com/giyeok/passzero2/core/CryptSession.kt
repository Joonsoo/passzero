package com.giyeok.passzero2.core

import com.google.protobuf.ByteString

class CryptSession(val revision: Long, private val secretKey: ByteString) {
  companion object {
    fun secretKeyFrom(localSecret: LocalInfoProto.LocalSecret, password: String): ByteString {
      val hash = Encryption.PasswordHasher.generateHash(Encryption.PasswordSalt(localSecret.passwordSalt), password)
      val folded = hash.bytes.substring(0, 32).xor(hash.bytes.substring(32, 64))

      return localSecret.localKey.xor(folded)
    }

    fun from(revision: Long, localSecret: LocalInfoProto.LocalSecret, password: String): CryptSession {
      return CryptSession(revision, secretKeyFrom(localSecret, password))
    }

    fun from(localInfoWithRevision: LocalInfoWithRevision, password: String): CryptSession =
      from(localInfoWithRevision.revision, localInfoWithRevision.localInfo.secret, password)
  }

  fun decode(bytes: ByteString): ByteString {
    check(bytes.size() >= Encryption.InitVector.INITVEC_LENGTH)
    val iv = Encryption.InitVector(bytes.substring(0, Encryption.InitVector.INITVEC_LENGTH))
    val body = bytes.substring(Encryption.InitVector.INITVEC_LENGTH)
    return Encryption.AES256CBC.decode(body, secretKey, iv)
  }

  fun encode(bytes: ByteString): ByteString {
    val iv = Encryption.InitVector.generate()
    val encoded = Encryption.AES256CBC.encode(bytes, secretKey, iv)
    return iv.bytes.concat(encoded)
  }
}
