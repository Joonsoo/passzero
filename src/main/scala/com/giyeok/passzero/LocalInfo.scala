package com.giyeok.passzero

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import com.giyeok.passzero.ByteArrayUtil._
import com.giyeok.passzero.Security.AES256CBC
import com.giyeok.passzero.Security.PasswordHash
import com.giyeok.passzero.storage.StorageProfile

object LocalInfo {
    // LocalInfo 파일 구조:
    // 첫 4바이트는 매직넘버 GYPZ
    // 그 다음 2바이트는 버젼값. 기본 0001
    // 그 다음 8바이트는 이 파일이 저장된 시점의 timestamp(참고정보)

    // 그 다음 32바이트는 LocalInfo용 password hash salt
    //  - 이 salt를 이용해 password로 LocalInfo 해석을 위한 키를 만든다
    // 그 다음 16바이트는 LocalInfo용 initial vector
    //  - LocalInfo의 나머지 내용을 해석할 때는 위의 salt와 비밀번호로 생성된 키 + 이 iv를 사용한다

    // 그 다음 (password) + (LocalInfo용 salt)로 만든 해시를 키로, (LocalInfo용 iv)를 iv로 하는 AES256CBC 로 암호화된 LocalKeys.toBytes
    //  - 원본은 80바이트, 암호화된 사이즈는 96바이트
    // 그 다음 storageProfile 정보를 Array[Byte]를 위의 localKeys와 동일한 키/iv를 이용해 AES256CBC로 암호화한 내용
    //  - storageProfile 정보는 null-ended storageType과 storageProfile이 정의한 내용으로 구성된다
    //  - 크기 확정 불가

    def save(password: String, localInfo: LocalInfo): Array[Byte] = {
        // localInfoSalt랑 localInfoIv는 여기서 랜덤하게 생성
        val rawLocalKeysBytes = localInfo.localKeys.toBytes
        val rawStorageProfileBytes = localInfo.storageProfile.toBytes
        ???
    }

    def save(password: String, localInfo: LocalInfo, dest: File): Unit = {
        if (!dest.isFile) {
            // TODO 오류 처리
            ???
        }
        val os = new FileOutputStream(dest)
        try {
            os.write(save(password, localInfo))
        } finally {
            os.close()
        }
    }

    case class LoadException(msg: String) extends Exception(msg)

    def load(password: String, input: Array[Byte]): LocalInfo = {
        if (input(0) == 'G' && input(1) == 'Y' && input(2) == 'P' && input(3) == 'Z') {
            val versionNum = (input(4), input(5))
            versionNum match {
                case (0, 1) =>
                    val timestamp = input.slice(6, 6 + 8).asLong
                    val localInfoSalt = input.slice(14, 14 + 32)
                    val localInfoIv = input.slice(44, 44 + 16)
                    assert(localInfoSalt.length == 32 && localInfoIv.length == 16)
                    val localInfoKey = PasswordHash.generateHash(localInfoSalt, password)

                    val encodedLocalKeys = input.slice(60, 60 + 96)
                    val localKeysBytes = AES256CBC.decode(encodedLocalKeys, localInfoKey, localInfoIv)
                    val localKeys = LocalKeys.fromBytes(localKeysBytes)

                    val encodedStorageProfile = input.slice(156, input.length)
                    val storageProfileBytes = AES256CBC.decode(encodedStorageProfile, localInfoKey, localInfoIv)
                    val (storageProfileTypeBytes, storageProfileContent) = storageProfileBytes span { _ == 0 }
                    val storageProfile = StorageProfile.fromBytes(storageProfileTypeBytes.asString, storageProfileContent)
                    new LocalInfo(timestamp, localKeys, storageProfile)

                case _ =>
                    throw LoadException(s"unknown version number: $versionNum")
            }
        } else {
            throw LoadException("invalid magic")
        }
    }

    def load(password: String, src: File): LocalInfo = {
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

class LocalInfo(val timestamp: Long, val localKeys: LocalKeys, val storageProfile: StorageProfile)
