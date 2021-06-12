package com.giyeok.passzero2.core

import com.google.protobuf.ByteString
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Encryption {
  fun secureRandom(length: Int): ByteString {
    val sr = SecureRandom.getInstance("SHA1PRNG")
    val random = ByteArray(length)
    sr.nextBytes(random)
    return ByteString.copyFrom(random)
  }

  data class InitVector(val bytes: ByteString) {
    companion object {
      const val INITVEC_LENGTH = 16
      fun generate(): InitVector = InitVector(secureRandom(INITVEC_LENGTH))
    }

    init {
      check(bytes.size() == INITVEC_LENGTH)
    }
  }

  object AES256CBC {
    fun encode(src: ByteString, key: ByteString, iv: InitVector): ByteString {
      check(key.size() == 32)
      val secureKey = SecretKeySpec(key.toByteArray(), "AES")
      val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
      c.init(Cipher.ENCRYPT_MODE, secureKey, IvParameterSpec(iv.bytes.toByteArray()))
      return ByteString.copyFrom(c.doFinal(src.toByteArray()))
    }

    fun decode(src: ByteString, key: ByteString, iv: InitVector): ByteString {
      check(key.size() == 32)
      val secureKey = SecretKeySpec(key.toByteArray(), "AES")
      val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
      c.init(Cipher.DECRYPT_MODE, secureKey, IvParameterSpec(iv.bytes.toByteArray()))
      return ByteString.copyFrom(c.doFinal(src.toByteArray()))
    }
  }

  object AES256EBC {
    fun encode(src: ByteString, key: ByteString, iv: InitVector): ByteString {
      check(key.size() == 32)
      val secureKey = SecretKeySpec(key.toByteArray(), "AES")
      val c = Cipher.getInstance("AES/EBC/PKCS5Padding")
      c.init(Cipher.ENCRYPT_MODE, secureKey, IvParameterSpec(iv.bytes.toByteArray()))
      return ByteString.copyFrom(c.doFinal(src.toByteArray()))
    }

    fun decode(src: ByteString, key: ByteString, iv: InitVector): ByteString {
      check(key.size() == 32)
      val secureKey = SecretKeySpec(key.toByteArray(), "AES")
      val c = Cipher.getInstance("AES/EBC/PKCS5Padding")
      c.init(Cipher.DECRYPT_MODE, secureKey, IvParameterSpec(iv.bytes.toByteArray()))
      return ByteString.copyFrom(c.doFinal(src.toByteArray()))
    }
  }

  object PasswordHasher {
    const val HASH_ITERATIONS = 10000

    fun generateHashAndSalt(password: String): PasswordPair {
      val salt = PasswordSalt(secureRandom(32))
      val hash = generateHash(salt, password)
      return PasswordPair(hash, salt)
    }

    fun generateHash(salt: PasswordSalt, password: String): PasswordHash {
      val spec = PBEKeySpec(password.toCharArray(), salt.bytes.toByteArray(), HASH_ITERATIONS, 64 * 8)
      val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
      return PasswordHash(ByteString.copyFrom(skf.generateSecret(spec).encoded))
    }
  }

  data class PasswordHash(val bytes: ByteString) {
    init {
      check(bytes.size() == 64)
    }
  }

  data class PasswordSalt(val bytes: ByteString) {
    init {
      check(bytes.size() == 32)
    }
  }

  data class PasswordPair(val hash: PasswordHash, val salt: PasswordSalt)
}
