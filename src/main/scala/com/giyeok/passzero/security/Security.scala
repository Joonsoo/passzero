package com.giyeok.passzero.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import com.giyeok.passzero.ByteArrayUtil.Implicit

object Security {
    def secureRandom(length: Int): Array[Byte] = {
        val sr = SecureRandom.getInstance()
        val random = new Array[Byte](length)
        sr.nextBytes(random)
        random
    }

    def generateRandomLocalInfo(): LocalInfo = {
        LocalInfo(secureRandom(32), secureRandom(32), secureRandom(16))
    }

    val pbkdf2Iterations = 10000

    def generatePasswordHashAndSalt(password: String, iterations: Int): (Array[Byte], Array[Byte]) = {
        val salt = secureRandom(32)
        val hash = generatePasswordHash(salt, password, iterations)
        (hash, salt)
    }

    def generatePasswordHash(salt: Array[Byte], password: String, iterations: Int): Array[Byte] = {
        val chars = password.toCharArray
        val spec = new PBEKeySpec(chars, salt, iterations, 64 * 8)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        skf.generateSecret(spec).getEncoded
    }

    object AES256CBC {
        def encode(src: Array[Byte], initialVector: Array[Byte], key: Array[Byte]): Array[Byte] = {
            val secureKey = new SecretKeySpec(key, "AES")
            val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
            c.init(Cipher.ENCRYPT_MODE, secureKey, new IvParameterSpec(initialVector))

            c.doFinal(src)
        }

        def decode(src: Array[Byte], initialVector: Array[Byte], key: Array[Byte]): Array[Byte] = {
            val secureKey = new SecretKeySpec(key, "AES")
            val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
            c.init(Cipher.DECRYPT_MODE, secureKey, new IvParameterSpec(initialVector))

            c.doFinal(src)
        }
    }

    object AES256EBC {
        def encode(src: Array[Byte], key: Array[Byte]): Array[Byte] = {
            val secureKey = new SecretKeySpec(key, "AES")
            val c = Cipher.getInstance("AES/EBC/PKCS5Padding")
            c.init(Cipher.ENCRYPT_MODE, secureKey)

            c.doFinal(src)
        }

        def decode(src: Array[Byte], key: Array[Byte]): Array[Byte] = {
            val secureKey = new SecretKeySpec(key, "AES")
            val c = Cipher.getInstance("AES/EBC/PKCS5Padding")
            c.init(Cipher.DECRYPT_MODE, secureKey)

            c.doFinal(src)
        }
    }

    private def generateSecureKey(password: String, pwSalt: Array[Byte], localKey: Array[Byte]): Array[Byte] = {
        val pwHash = generatePasswordHash(pwSalt, password, pbkdf2Iterations)
        val halfPwHash = (pwHash take 32) xor (pwHash drop 32)

        localKey xor halfPwHash
    }

    def encodeText(password: String, localInfo: LocalInfo, text: String): Array[Byte] = {
        val secureKey = generateSecureKey(password, localInfo.pwSalt, localInfo.localKey)
        AES256CBC.encode(text.getBytes("UTF-8"), localInfo.localIv, secureKey)
    }

    def decodeText(password: String, localInfo: LocalInfo, source: Array[Byte]): String = {
        val secureKey = generateSecureKey(password, localInfo.pwSalt, localInfo.localKey)
        new String(AES256CBC.decode(source, localInfo.localIv, secureKey), "UTF-8")
    }
}
