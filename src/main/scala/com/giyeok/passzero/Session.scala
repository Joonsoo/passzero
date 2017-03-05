package com.giyeok.passzero

import java.io.File
import com.giyeok.passzero.utils.ByteArrayUtil._
import com.giyeok.passzero.Security.AES256CBC
import com.giyeok.passzero.Security.PasswordHash
import com.giyeok.passzero.storage.Entity
import com.giyeok.passzero.storage.EntityMeta
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.storage.StorageSession
import scala.concurrent.duration._
import com.giyeok.passzero.Security.InitVec
import com.giyeok.passzero.storage.StorageProfile
import org.json4s.JValue

object Session {
    def load(password: String, localInfoFile: File): Session = {
        val (localInfoFileTimestamp, localInfo) = LocalInfo.load(password, localInfoFile)

        val localInfoFileAge = Timestamp.current - localInfoFileTimestamp
        // localInfoFileTimestamp 를 보고 파일이 너무 오래되었으면(60일 이상 지났으면) 새로운 salt와 iv로 업데이트
        // TODO 그런데 이 작업을 여기서 하는게 맞을까?
        if (localInfoFileAge > 60.days) {
            LocalInfo.save(password, localInfo)
        }

        // TODO Session에서 storageProfile이 변경될 때 localInfoFile을 업데이트할 수 있도록 Session이 localInfoFile에 대한 정보를 갖고 있어야 한다
        new Session(password, localInfo.localSecret, localInfo.storageProfile)
    }
}

class StorageSessionManager(_storageProfile: StorageProfile) {
    private var _session = _storageProfile.createSession(this)

    def storageSession(): StorageSession = this.synchronized { _session }
}

class Session(password: String, localKeys: LocalSecret, storageSessionManager: StorageSessionManager) {
    def this(password: String, localKeys: LocalSecret, storageProfile: StorageProfile) =
        this(password, localKeys, new StorageSessionManager(storageProfile))

    private val secretKey: Array[Byte] = {
        // (password: String, pwSalt: Array[Byte], localKey: Array[Byte])
        val pwHash = PasswordHash.generateHash(localKeys.pwSalt, password)
        val halfPwHash = (pwHash take 32) xor (pwHash drop 32)

        localKeys.localKey xor halfPwHash
    }

    // TODO StorageSession에서 필요에 의해 session의 storage가 변경되어야 할 수도 있다
    def storage: StorageSession = storageSessionManager.storageSession()

    def localInfo: LocalInfo = new LocalInfo(localKeys, storage.profile)

    def encode(array: Array[Byte]): (InitVec, Array[Byte]) = {
        val iv = InitVec.generate()
        (iv, AES256CBC.encode(array, secretKey, iv))
    }

    def decode(source: Array[Byte], initVec: InitVec): Array[Byte] = {
        AES256CBC.decode(source, secretKey, initVec)
    }

    def list(path: Path): Seq[EntityMeta] = {
        // storage에서 (디렉토리) path 하위 Path들 목록
        storage.list(path)
    }

    def get(path: Path): Option[Entity[Array[Byte]]] = {
        // storage에서 (파일) path의 내용 디코딩해서 반환
        storage.get(path) map {
            _ mapContent { content =>
                val (initVecBytes, body) = content.splitAt(InitVec.length)
                decode(body, InitVec(initVecBytes))
            }
        }
    }

    def getAsString(path: Path): Option[Entity[String]] = {
        // storage에서 (파일) path의 내용 디코딩해서 스트링으로 변환해서 반환
        get(path) map { _ mapContent { _.asString } }
    }

    def getAsJson(path: Path): Option[Entity[JValue]] = {
        getAsString(path) map { _ mapContent { _ => ??? } }
    }

    def put(path: Path, content: Array[Byte]): Unit = {
        // storage에 (파일) path의 내용을 인코딩된 content로 치환
        val (initVec, encoded) = encode(content)
        storage.putContent(path, initVec.array ++ encoded)
    }

    def putString(path: Path, content: String): Unit = {
        put(path, content.toBytes)
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
