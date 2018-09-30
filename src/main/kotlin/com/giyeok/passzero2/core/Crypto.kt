package com.giyeok.passzero2.core

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Crypto {
    fun secureRandom(length: Int): ByteArray {
        val sr = SecureRandom.getInstance("SHA1PRNG")
        val random = ByteArray(length)
        sr.nextBytes(random)
        return random
    }

    object AES256CBC {
        val algorithm = "AES/CBC/PKCS5Padding"
        val keySize = 32

        object InitVec {
            val size = 16
            fun generate() = secureRandom(size)
        }

        fun encode(src: ByteArray, key: ByteArray, initVec: ByteArray): ByteArray {
            assert(key.size == keySize && initVec.size == InitVec.size)
            val secureKey = SecretKeySpec(key, "AES")
            val c = Cipher.getInstance(algorithm)
            c.init(Cipher.ENCRYPT_MODE, secureKey, IvParameterSpec(initVec))

            return c.doFinal(src)
        }

        fun decode(src: ByteArray, key: ByteArray, initVec: ByteArray): ByteArray {
            assert(key.size == keySize && initVec.size == InitVec.size)
            val secureKey = SecretKeySpec(key, "AES")
            val c = Cipher.getInstance(algorithm)
            c.init(Cipher.DECRYPT_MODE, secureKey, IvParameterSpec(initVec))

            return c.doFinal(src)
        }
    }

    object AES256EBC {
        val algorithm = "AES/EBC/PKCS5Padding"
        val keySize = 32

        fun encode(src: ByteArray, key: ByteArray): ByteArray {
            assert(key.size == keySize)
            val secureKey = SecretKeySpec(key, "AES")
            val c = Cipher.getInstance(algorithm)
            c.init(Cipher.ENCRYPT_MODE, secureKey)

            return c.doFinal(src)
        }

        fun decode(src: ByteArray, key: ByteArray): ByteArray {
            assert(key.size == keySize)
            val secureKey = SecretKeySpec(key, "AES")
            val c = Cipher.getInstance(algorithm)
            c.init(Cipher.DECRYPT_MODE, secureKey)

            return c.doFinal(src)
        }
    }

    object PasswordHash {
        val iterations = 10000
        val saltSize = 32
        val algorithm = "PBKDF2WithHmacSHA1"

        data class HashAndSalt(val hash: ByteArray, val salt: ByteArray)

        fun generateHashAndSalt(password: String): HashAndSalt {
            val salt = secureRandom(saltSize)
            val hash = generateHash(salt, password)
            return HashAndSalt(hash, salt)
        }

        fun generateHash(salt: ByteArray, password: String): ByteArray {
            assert(salt.size == saltSize)
            val chars = password.toCharArray()
            val spec = PBEKeySpec(chars, salt, iterations, 64 * 8)
            val skf = SecretKeyFactory.getInstance(algorithm)
            return skf.generateSecret(spec).encoded
        }
    }
}