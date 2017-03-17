package com.giyeok.passzero.storage

import com.giyeok.passzero.LocalInfo
import com.giyeok.passzero.StorageSessionManager

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
// TODO StorageProfile에서 toBytes가 변경될 상황이 생기면 LocalInfo를 새로 저장해야 함
trait StorageProfile {
    private var updateListeners: List[() => Unit] = List()
    def addUpdateListener(listener: () => Unit): Unit = {
        updateListeners +:= listener
    }
    def fireUpdated(): Unit = {
        updateListeners foreach { func => func() }
    }

    val name: String
    def infoText: String
    def toBytes: Array[Byte]
    def createSession(manager: StorageSessionManager): StorageSession
}
