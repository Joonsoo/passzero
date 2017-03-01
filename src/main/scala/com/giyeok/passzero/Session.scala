package com.giyeok.passzero

import java.io.File
import com.giyeok.passzero.ByteArrayUtil._
import com.giyeok.passzero.Security.AES256CBC
import com.giyeok.passzero.Security.PasswordHash
import com.giyeok.passzero.storage.Entity
import com.giyeok.passzero.storage.EntityMeta
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.storage.StorageProfile
import com.giyeok.passzero.storage.StorageSession

object Session {
    def load(password: String, localInfoPath: String): Session = {
        val localInfo = LocalInfo.load(password, new File(localInfoPath))
        new Session(password, localInfo.localKeys, localInfo.storageProfile.createSession())
    }
}

class Session(password: String, localKeys: LocalKeys, storage: StorageSession) {
    private val secureKey: Array[Byte] = {
        // (password: String, pwSalt: Array[Byte], localKey: Array[Byte])
        val pwHash = PasswordHash.generateHash(localKeys.pwSalt, password)
        val halfPwHash = (pwHash take 32) xor (pwHash drop 32)

        localKeys.localKey xor halfPwHash
    }

    def localInfo: (LocalKeys, StorageProfile) = (localKeys, storage.profile)

    def encode(array: Array[Byte]): Array[Byte] = {
        AES256CBC.encode(array, secureKey, localKeys.localIv)
    }

    def decode(source: Array[Byte]): Array[Byte] = {
        AES256CBC.decode(source, secureKey, localKeys.localIv)
    }

    def list(path: Path): Seq[EntityMeta] = {
        // storage에서 (디렉토리) path 하위 Path들 목록
        storage.list(path)
    }

    def get(path: Path): Entity[Array[Byte]] = {
        // storage에서 (파일) path의 내용 디코딩해서 반환
        storage.get(path) mapContent decode
    }

    def getAsString(path: Path): Entity[String] = {
        // storage에서 (파일) path의 내용 디코딩해서 스트링으로 변환해서 반환
        get(path) mapContent { _.asString }
    }

    def getAsJson(path: Path): Entity[Nothing] = {
        getAsString(path) mapContent { _ => ??? }
    }

    def put(path: Path, content: Array[Byte]): Unit = {
        // storage에 (파일) path의 내용을 인코딩된 content로 치환
        storage.putContent(path, encode(content))
    }

    def putString(path: Path, newContent: String): Unit = {
        put(path, newContent.toBytes)
    }

    def delete(path: Path, recursive: Boolean): Unit = {
        storage.delete(path, recursive)
    }

    def mkdir(path: Path, recursive: Boolean): Unit = {
        storage.mkdir(path, recursive)
    }

    // TODO checkAndPut이 정말 필요할까?
    def checkAndPut(path: Path, checksum: Array[Byte], newContent: String): Unit = {
        // storage에 (파일) path의 내용 체크섬이 checksum인지 확인하고 일치하면 newContent로 치환
        // checksum이 일치하지 않으면 익셉션 발생
        ???
    }
}
