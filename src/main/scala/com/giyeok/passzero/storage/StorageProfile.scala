package com.giyeok.passzero.storage

object StorageProfile {
    def fromBytes(storageType: String, bytes: Array[Byte]): StorageProfile = {
        Storages.types get storageType match {
            case Some(func) =>
                func(bytes)
            case _ =>
                throw new Exception(s"Unknown storage type: $storageType")
        }
    }
}

// StorageProfile 정보는 암호화돼서 LocalInfo와 같은 파일에 저장
trait StorageProfile {
    val name: String
    def toBytes: Array[Byte]
    def createSession(): StorageSession
}
