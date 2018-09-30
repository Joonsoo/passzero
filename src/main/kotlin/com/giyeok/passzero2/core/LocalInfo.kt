package com.giyeok.passzero2.core

import com.giyeok.passzero2.util.readAll
import com.giyeok.passzero2.util.readBytesFully
import java.io.*
import java.util.*

class LocalSecret(val pwSalt: ByteArray, val localKey: ByteArray) {
    init {
        assert(pwSalt.size == 32)
        assert(localKey.size == 32)
    }

    fun toBytes(): ByteArray = pwSalt + localKey
    fun toBase64(): String = Base64.getEncoder().encodeToString(toBytes())
    fun toAlphaDigits(): String {
        TODO()
    }

    fun toReadable(): String = toBase64().chunked(8).joinToString("-")
    fun contentEquals(other: LocalSecret): Boolean =
            pwSalt contentEquals other.pwSalt && localKey contentEquals other.localKey

    companion object {
        fun fromBytes(bytes: ByteArray): LocalSecret {
            assert(bytes.size == 64)
            return LocalSecret(bytes.sliceArray(0..31), bytes.sliceArray(32..63))
        }

        fun fromBase64(string: String): LocalSecret = fromBytes(Base64.getDecoder().decode(string))
        fun generateRandomLocalInfo(): LocalSecret =
                LocalSecret(Crypto.secureRandom(32), Crypto.secureRandom(32))
    }
}

class LocalInfo(
        val timestamp: Long,
        val revision: Long,
        val localSecret: LocalSecret,
        val storageProfile: StorageProfile) {
    // LocalInfo 파일 구조:
    // 첫 4바이트는 매직넘버 GYPZ
    // 그 다음 2바이트는 버젼값. 기본 0001
    // 그 다음 8바이트는 이 파일이 저장된 시점의 timestamp(참고정보)
    // 그 다음 8바이트는 revision number

    // 그 다음 32바이트는 LocalInfo용 password hash salt
    //  - 이 salt를 이용해 password로 LocalInfo 해석을 위한 키를 만든다
    // 그 다음 16바이트는 LocalInfo용 initial vector
    //  - LocalInfo의 나머지 내용을 해석할 때는 위의 salt와 비밀번호로 생성된 키 + 이 iv를 사용한다

    // 그 다음에는 64바이트의 LocalInfo.toBytes + storageName + null byte + storageProfileInfo 의 정보를
    // (password) + (LocalInfo용 salt)로 만든 해시를 키로, (LocalInfo용 iv)를 iv로 하는 AES256CBC 로 암호해서 저장
    //  - 크기는 storageProfileInfo때문에 확정 불가

    // encode는 호출될 때마다 내부적으로 random salt와 iv를 생성하기 때문에 같은 인자를 줘도 결과가 항상 다르다
    fun encode(password: String): ByteArray {
        val arrayStream = ByteArrayOutputStream()
        val stream = DataOutputStream(arrayStream)

        stream.writeByte('G'.toInt())
        stream.writeByte('Y'.toInt())
        stream.writeByte('P'.toInt())
        stream.writeByte('Z'.toInt())
        stream.writeByte(0)
        stream.writeByte(1)
        stream.writeLong(this.timestamp)
        stream.writeLong(this.revision)

        val (hash, salt) = Crypto.PasswordHash.generateHashAndSalt(password)
        stream.write(salt)

        val iv = Crypto.AES256CBC.InitVec.generate()
        stream.write(iv)

        val localInfoEncodeKey = hash.sliceArray(0 until 32)

        val contentBuf = ByteArrayOutputStream(100)
        val contentStream = DataOutputStream(contentBuf)
        contentStream.write(localSecret.toBytes())
        contentStream.writeBytes(storageProfile.spec.name)
        contentStream.write(0)
        contentStream.write(storageProfile.toBytes())

        val encodedContent = Crypto.AES256CBC.encode(contentBuf.toByteArray(), localInfoEncodeKey, iv)
        stream.write(encodedContent)

        return arrayStream.toByteArray()
    }

    fun save(password: String, dest: File) {
        if (dest.exists() && !dest.isFile) {
            TODO("error")
        }

        val encoded = encode(password)
        FileOutputStream(dest).use { os ->
            os.write(encoded)
        }
    }

    infix fun contentEquals(other: LocalInfo): Boolean =
            this.timestamp == other.timestamp && this.revision == other.revision &&
                    this.localSecret.contentEquals(other.localSecret) &&
                    this.storageProfile.contentEquals(other.storageProfile)

    companion object {
        class DecodeException(msgKey: String) : Exception(msgKey)

        fun decode(password: String, encoded: ByteArray): LocalInfo {
            val stream = DataInputStream(ByteArrayInputStream(encoded))

            // 1. Magic Number
            val magic = ByteArray(4)
            stream.read(magic, 0, 4)
            if (!magic.contentEquals("GYPZ".toByteArray())) {
                throw DecodeException("Wrong magic number")
            }

            // 2. Version Number
            val versionNum = ByteArray(2)
            stream.read(versionNum, 0, 2)
            if (!versionNum.contentEquals(byteArrayOf(0, 1))) {
                throw DecodeException("Unknown version: $versionNum")
            }

            // 3. Timestamp 8 bytes
            val timestamp = stream.readLong()
            // 4. Revision 8 bytes
            val revision = stream.readLong()
            // 5. Local Info Salt
            val localInfoSalt = stream.readBytesFully(Crypto.PasswordHash.saltSize)
            // 6. Local Info IV
            val localInfoIv = stream.readBytesFully(Crypto.AES256CBC.InitVec.size)
            val localInfoKey = Crypto.PasswordHash.generateHash(localInfoSalt, password).sliceArray(0 until 32)

            // 7. Encoded Content
            val encodedContent = stream.readAll()
            val content = Crypto.AES256CBC.decode(encodedContent, localInfoKey, localInfoIv)

            // 8. Local Secret Info
            val localSecretBytes = content.sliceArray(0 until 64)
            val localSecret = LocalSecret.fromBytes(localSecretBytes)

            // 9. Session Profile Info
            val sessionProfileBytes = content.sliceArray(64 until content.size)
            val sessionProfile = StorageProfile.fromBytes(sessionProfileBytes)

            return LocalInfo(timestamp, revision, localSecret, sessionProfile)
        }

        fun load(password: String, src: File): LocalInfo {
            if (!src.exists() || !src.isFile) {
                TODO("error")
            }

            val encoded = src.readBytes()
            return decode(password, encoded)
        }
    }
}
