package com.giyeok.passzero.storage.googledrive

import com.giyeok.passzero.storage.Entity
import com.giyeok.passzero.storage.EntityMeta
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.storage.StorageProfile
import com.giyeok.passzero.storage.StorageSession

class GoogleDriveStorageSession extends StorageSession {
    def profile: StorageProfile = ???

    def list(path: Path): Seq[EntityMeta] = ???

    def get(path: Path): Option[Entity[Array[Byte]]] = ???

    def putContent(path: Path, content: Array[Byte]): Unit = ???

    def delete(path: Path, recursive: Boolean): Boolean = ???

    def mkdir(path: Path, recursive: Boolean): Unit = ???
}
