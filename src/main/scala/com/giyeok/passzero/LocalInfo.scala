package com.giyeok.passzero

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Date
import scala.concurrent.duration._
import com.giyeok.passzero.utils.ByteArrayUtil._
import com.giyeok.passzero.Security.AES256CBC
import com.giyeok.passzero.Security.InitVec
import com.giyeok.passzero.Security.PasswordHash
import com.giyeok.passzero.storage.StorageProfile
import com.giyeok.passzero.utils.ByteBuf
import com.giyeok.passzero.utils.ByteReader

object LocalInfo {
    // LocalInfo 파일 구조:
    // 첫 4바이트는 매직넘버 GYPZ
    // 그 다음 2바이트는 버젼값. 기본 0001
    // 그 다음 8바이트는 이 파일이 저장된 시점의 timestamp(참고정보)

    // 그 다음 32바이트는 LocalInfo용 password hash salt
    //  - 이 salt를 이용해 password로 LocalInfo 해석을 위한 키를 만든다
    // 그 다음 16바이트는 LocalInfo용 initial vector
    //  - LocalInfo의 나머지 내용을 해석할 때는 위의 salt와 비밀번호로 생성된 키 + 이 iv를 사용한다

    // 그 다음에는 64바이트의 LocalInfo.toBytes + storageName + null byte + storageProfileInfo 의 정보를
    // (password) + (LocalInfo용 salt)로 만든 해시를 키로, (LocalInfo용 iv)를 iv로 하는 AES256CBC 로 암호해서 저장
    //  - 크기는 storageProfileInfo때문에 확정 불가

    // save는 호출될 때마다 random salt와 iv를 생성하기 때문에 같은 인자를 줘도 결과가 항상 다르다
    def save(password: String, localInfo: LocalInfo): Array[Byte] = {
        // localInfoSalt랑 localInfoIv는 여기서 랜덤하게 생성
        // LocalInfo 파일의 timestamp는 이 메소드가 실행되는 시점의 실제 타임스탬프로 하면 됨
        val timestamp = Timestamp.current

        val buf = new ByteBuf(120)
        buf.writeBytes(Seq[Byte]('G', 'Y', 'P', 'Z'))
        buf.writeBytes(Seq[Byte](0, 1))
        buf.writeLong(timestamp.date)

        val (pwHash, pwSalt) = Security.PasswordHash.generateHashAndSalt(password)
        buf.writeBytes(pwSalt) ensuring pwSalt.length == 32
        val iv = InitVec.generate()
        buf.writeBytes(iv.array) ensuring iv.array.length == InitVec.length

        val localInfoEncodeKey = pwHash take 32

        val contentBuf = new ByteBuf(100)
        val rawLocalKeysBytes = localInfo.localSecret.toBytes
        val rawStorageProfileBytes = localInfo.storageProfile.toBytes
        contentBuf.writeBytes(rawLocalKeysBytes) ensuring rawLocalKeysBytes.length == 64
        contentBuf.writeString(localInfo.storageProfile.name)
        contentBuf.writeByte(0)
        contentBuf.writeBytes(rawStorageProfileBytes)

        val encodedContent = Security.AES256CBC.encode(contentBuf.finish(), localInfoEncodeKey, iv)
        buf.writeBytes(encodedContent)

        buf.finish()
    }

    def save(password: String, localInfo: LocalInfo, dest: File): Unit = {
        if (dest.exists() && !dest.isFile) {
            // TODO 오류 처리
            ???
        }
        val savingBytes = save(password, localInfo)
        val os = new FileOutputStream(dest)
        try {
            os.write(savingBytes)
        } finally {
            os.close()
        }
    }

    case class LoadException(msg: String) extends Exception(msg)

    def load(password: String, input: Array[Byte]): (Timestamp, LocalInfo) = {
        val reader = new ByteReader(input)
        if (reader.readBytes(4).toSeq == Seq[Byte]('G', 'Y', 'P', 'Z')) {
            val versionNum = reader.readBytes(2).toSeq
            versionNum match {
                case Seq(0, 1) =>
                    val timestamp = Timestamp(reader.readLong())
                    val localInfoSalt = reader.readBytes(32)
                    val localInfoIv = InitVec(reader.readBytes(InitVec.length))
                    assert(localInfoSalt.length == 32)
                    val localInfoKey = PasswordHash.generateHash(localInfoSalt, password) take 32

                    val encodedContent = reader.readRest()
                    val content = AES256CBC.decode(encodedContent, localInfoKey, localInfoIv)

                    val (localKeysBytes, storageProfileBytes) = content splitAt 64

                    val localKeys = LocalSecret.fromBytes(localKeysBytes)
                    val (storageProfileTypeBytes, storageProfileContent) = storageProfileBytes span { _ != 0 }
                    val storageProfileType = storageProfileTypeBytes.asString
                    val storageProfile = StorageProfile.fromBytes(storageProfileType, storageProfileContent.tail)

                    (timestamp, new LocalInfo(localKeys, storageProfile))

                case _ =>
                    throw LoadException(s"unknown version number: $versionNum")
            }
        } else {
            throw LoadException("invalid magic")
        }
    }

    def load(password: String, src: File): (Timestamp, LocalInfo) = {
        if (!src.isFile) {
            // TODO 오류 처리
            ???
        }

        // TODO src.length() 참고하지 말고 그냥 끝까지 읽도록 수정
        val length = src.length().toInt

        val is = new FileInputStream(src)
        val input = new Array[Byte](length)
        try {
            val readBytes = is.read(input)
            if (readBytes != length) {
                // TODO 오류 처리
                ???
            }
            val restInput = is.read()
            if (restInput != -1) {
                // TODO 오류 처리
                ???
            }
        } finally {
            is.close()
        }
        load(password, input)
    }
}

object Timestamp {
    def current = Timestamp(System.currentTimeMillis())
}
case class Timestamp(date: Long) extends AnyVal {
    def -(other: Timestamp): Duration = (date - other.date).millis
    def toDate: Date = new Date(date)
}

class LocalInfo(val localSecret: LocalSecret, val storageProfile: StorageProfile)
