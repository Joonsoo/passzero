package com.giyeok.passzero

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Main {
    implicit class ByteArrayImplicit(array: Array[Byte]) {
        def toHexString: String =
            (array map { x => f"$x%02x" }).mkString

        def toBase64: String =
            Base64.getEncoder.encodeToString(array)

        def xor(other: Array[Byte]): Array[Byte] = {
            val newArray = new Array[Byte](Math.max(array.length, other.length))
            for (i <- newArray.indices) {
                val a = if (i < array.length) array(i) else 0
                val b = if (i < other.length) other(i) else 0
                newArray(i) = (a ^ b).toByte
            }
            newArray
        }
    }

    def secureRandom(length: Int): Array[Byte] = {
        val sr = SecureRandom.getInstance("SHA1PRNG")
        val random = new Array[Byte](length)
        sr.nextBytes(random)
        random
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

    object AES256 {
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

    case class LocalInfo(pwSalt: Array[Byte], localKey: Array[Byte], localIv: Array[Byte]) {
        assert(pwSalt.length == 32)
        assert(localKey.length == 32)
        assert(localIv.length == 16)
        def toReadable: String = {
            val array = pwSalt ++ localKey ++ localIv
            assert(array.length == 80)
            // 80 bytes = 640 bits
            // 5bit = 32 chars, 640 / 5 = 128
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            assert(chars.length >= 32)

            val grouped = array.grouped(5).toSeq
            // 5바이트씩 쪼개면 한 덩어리에서 8글자가 나옴
            assert(grouped forall { _.length == 5 })
            assert(grouped.length == 16)

            val rawString = (grouped flatMap { y =>
                val x = y map { _.toInt & 0xff }
                val indices = Seq(
                    x(0) >> 3,
                    ((x(0) & 0x7) << 2) | (x(1) >> 6),
                    (x(1) >> 1) & 0x1f,
                    ((x(1) & 0x1) << 4) | (x(2) >> 4),
                    ((x(2) & 0xf) << 1) | (x(3) >> 7),
                    (x(3) >> 2) & 0x1f,
                    ((x(3) & 0x3) << 3) | (x(4) >> 5),
                    x(4) & 0x1f
                )
                // println(indices)
                indices map { chars(_) }
            }).mkString
            assert(rawString.length == 128)

            rawString.grouped(8) mkString "-"
        }

        // LocalInfo를 저장할 때는 비밀번호로 한 번 더 암호화 해서
    }

    def generateRandomLocalInfo(): LocalInfo = {
        LocalInfo(secureRandom(32), secureRandom(32), secureRandom(16))
    }

    def generateSecureKey(password: String, pwSalt: Array[Byte], localKey: Array[Byte]): Array[Byte] = {
        val pwHash = generatePasswordHash(pwSalt, password, pbkdf2Iterations)
        val halfPwHash = (pwHash take 32) xor (pwHash drop 32)

        localKey xor halfPwHash
    }

    def encodeText(password: String, localInfo: LocalInfo, text: String): Array[Byte] = {
        val secureKey = generateSecureKey(password, localInfo.pwSalt, localInfo.localKey)
        AES256.encode(text.getBytes("UTF-8"), localInfo.localIv, secureKey)
    }

    def decodeText(password: String, localInfo: LocalInfo, source: Array[Byte]): String = {
        val secureKey = generateSecureKey(password, localInfo.pwSalt, localInfo.localKey)
        new String(AES256.decode(source, localInfo.localIv, secureKey), "UTF-8")
    }

    def main(args: Array[String]): Unit = {
        val localInfo = generateRandomLocalInfo()
        val password = "hello 123"

        println(s"localInfo:${localInfo.toReadable}")

        val original = "Joonsoo is real king"
        val encoded = encodeText(password, localInfo, original)
        println(s"encoded(${original.getBytes("UTF-8").length} -> ${encoded.length}):${encoded.toHexString}")
        val decoded = decodeText(password, localInfo, encoded)
        println(s"decoded:$decoded;")
    }
}
