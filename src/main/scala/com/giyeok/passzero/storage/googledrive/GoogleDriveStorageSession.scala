package com.giyeok.passzero.storage.googledrive

import com.giyeok.passzero.StorageSessionManager
import com.giyeok.passzero.storage.Entity
import com.giyeok.passzero.storage.EntityMeta
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.storage.StorageProfile
import com.giyeok.passzero.storage.StorageSession
import com.google.api.services.drive.Drive

class GoogleDriveStorageSession(
        val profile: StorageProfile,
        val manager: StorageSessionManager,
        drive: Drive
) extends StorageSession {
    def list(path: Path): Seq[EntityMeta] = ???

    def get(path: Path): Option[Entity[Array[Byte]]] = ???

    def putContent(path: Path, content: Array[Byte]): Unit = ???

    def delete(path: Path, recursive: Boolean): Boolean = ???

    def mkdir(path: Path, recursive: Boolean): Unit = ???
}
