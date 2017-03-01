package com.giyeok.passzero

import java.io.File
import com.giyeok.passzero.ByteArrayUtil._
import com.giyeok.passzero.Security.AES256CBC
import com.giyeok.passzero.Security.PasswordHash
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.storage.StorageSession
import com.giyeok.passzero.storage.StorageSessionFactory

object Session {
    def create(password: String, localInfoPath: String, storageSessionFactory: StorageSessionFactory): Session = {
        val (localKeys, storageAuth) = LocalInfo.load(password, new File(localInfoPath))
        new Session(password, localKeys, storageSessionFactory.create(storageAuth))
    }
}

class Session(password: String, localKeys: LocalKeys, storage: StorageSession) {
    private val secureKey: Array[Byte] = {
        // (password: String, pwSalt: Array[Byte], localKey: Array[Byte])
        val pwHash = PasswordHash.generateHash(localKeys.pwSalt, password)
        val halfPwHash = (pwHash take 32) xor (pwHash drop 32)

        localKeys.localKey xor halfPwHash
    }

    def encodeText(text: String): Array[Byte] = {
        AES256CBC.encode(text.toBytes, secureKey, localKeys.localIv)
    }

    def decodeText(source: Array[Byte]): String = {
        AES256CBC.decode(source, secureKey, localKeys.localIv).asString
    }

    def list(path: Path): Seq[Path] = {
        // storage에서 (디렉토리) path 하위 Path들 목록
        ???
    }

    def get(path: Path): String = {
        // storage에서 (파일) path의 내용 디코딩해서 반환
        ???
    }

    def put(path: Path, newContent: String): Unit = {
        // storage에 (파일) path의 내용 newContent로 치환
        ???
    }

    def checkAndPut(path: Path, checksum: Array[Byte], newContent: String): Unit = {
        // storage에 (파일) path의 내용 체크섬이 checksum인지 확인하고 일치하면 newContent로 치환
        // checksum이 일치하지 않으면 익셉션 발생
        ???
    }
}
