package com.giyeok.passzero.storage.local

import com.giyeok.passzero.StorageSessionManager
import com.giyeok.passzero.storage.Entity
import com.giyeok.passzero.storage.EntityMeta
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.storage.StorageSession

class LocalStorageSession(
        val profile: LocalStorageProfile,
        manager: StorageSessionManager
) extends StorageSession {
    def list(path: Path): Stream[EntityMeta] = ???

    def get(path: Path): Option[Entity[Array[Byte]]] = ???

    def putContent(path: Path, content: Array[Byte]): Unit = ???

    def delete(path: Path, recursive: Boolean): Boolean = ???

    def mkdir(path: Path, recursive: Boolean): Unit = ???
}
