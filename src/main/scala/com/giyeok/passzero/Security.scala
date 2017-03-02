package com.giyeok.passzero

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Security {
    def secureRandom(length: Int): Array[Byte] = {
        val sr = SecureRandom.getInstance("SHA1PRNG")
        val random = new Array[Byte](length)
        sr.nextBytes(random)
        random
    }

    object InitVec {
        val length = 16
        def generate(): InitVec =
            InitVec(secureRandom(length))
    }
    case class InitVec(array: Array[Byte]) {
        assert(array.length == InitVec.length)
    }

    object AES256CBC {
        def encode(src: Array[Byte], key: Array[Byte], initialVector: InitVec): Array[Byte] = {
            assert(key.length == 32)
            val secureKey = new SecretKeySpec(key, "AES")
            val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
            c.init(Cipher.ENCRYPT_MODE, secureKey, new IvParameterSpec(initialVector.array))

            c.doFinal(src)
        }

        def decode(src: Array[Byte], key: Array[Byte], initialVector: InitVec): Array[Byte] = {
            assert(key.length == 32)
            val secureKey = new SecretKeySpec(key, "AES")
            val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
            c.init(Cipher.DECRYPT_MODE, secureKey, new IvParameterSpec(initialVector.array))

            c.doFinal(src)
        }
    }

    object AES256EBC {
        def encode(src: Array[Byte], key: Array[Byte]): Array[Byte] = {
            assert(key.length == 32)
            val secureKey = new SecretKeySpec(key, "AES")
            val c = Cipher.getInstance("AES/EBC/PKCS5Padding")
            c.init(Cipher.ENCRYPT_MODE, secureKey)

            c.doFinal(src)
        }

        def decode(src: Array[Byte], key: Array[Byte]): Array[Byte] = {
            assert(key.length == 32)
            val secureKey = new SecretKeySpec(key, "AES")
            val c = Cipher.getInstance("AES/EBC/PKCS5Padding")
            c.init(Cipher.DECRYPT_MODE, secureKey)

            c.doFinal(src)
        }
    }

    object PasswordHash {
        val iterations = 10000

        def generateHashAndSalt(password: String): (Array[Byte], Array[Byte]) = {
            val salt = secureRandom(32)
            val hash = generateHash(salt, password)
            (hash, salt)
        }

        def generateHash(salt: Array[Byte], password: String): Array[Byte] = {
            assert(salt.length == 32)
            val chars = password.toCharArray
            val spec = new PBEKeySpec(chars, salt, iterations, 64 * 8)
            val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            skf.generateSecret(spec).getEncoded
        }
    }
}
