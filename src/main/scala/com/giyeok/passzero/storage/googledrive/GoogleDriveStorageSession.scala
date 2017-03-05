package com.giyeok.passzero.storage.googledrive

import com.giyeok.passzero.StorageSessionManager
import com.giyeok.passzero.storage.Entity
import com.giyeok.passzero.storage.EntityMeta
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.storage.StorageProfile
import com.giyeok.passzero.storage.StorageSession
import com.google.api.services.drive.Drive
import scala.collection.JavaConverters._

// 만료되거나 하면 새로 갱신해야 할 것
// manager를 통해서 Storage session 객체를 변경. dataStore에 저장되는 내용은 StorageProfile에서 알아서 처리
class GoogleDriveStorageSession(
        val profile: GoogleDriveStorageProfile,
        applicationRoot: Path,
        manager: StorageSessionManager,
        drive: Drive
) extends StorageSession {
    def list(path: Path): Seq[EntityMeta] = {
        val list = drive.files().list().execute()
        list.getFiles.iterator().asScala.toSeq map { f =>
            EntityMeta(path \ f.getName, Map("id" -> f.getId))
        }
    }

    def get(path: Path): Option[Entity[Array[Byte]]] = ???

    def putContent(path: Path, content: Array[Byte]): Unit = {
    }

    def delete(path: Path, recursive: Boolean): Boolean = ???

    def mkdir(path: Path, recursive: Boolean): Unit = ???
}
