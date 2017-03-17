package com.giyeok.passzero.storage.googledrive

import scala.collection.JavaConverters._
import scala.concurrent.Future
import com.giyeok.passzero.StorageSessionManager
import com.giyeok.passzero.storage.Entity
import com.giyeok.passzero.storage.EntityMeta
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.storage.StorageSession
import com.giyeok.passzero.utils.FutureStream
import com.google.api.services.drive.Drive

// 만료되거나 하면 새로 갱신해야 할 것
// manager를 통해서 Storage session 객체를 변경. dataStore에 저장되는 내용은 StorageProfile에서 알아서 처리
class GoogleDriveStorageSession(
        val profile: GoogleDriveStorageProfile,
        applicationRoot: Path,
        manager: StorageSessionManager,
        drive: Drive
) extends StorageSession {
    def list(path: Path): FutureStream[Seq[EntityMeta]] = {
        val list = drive.files().list().execute()
        val files = list.getFiles.iterator().asScala.toSeq
        val page = files map { f => EntityMeta(path / f.getName, f.getId, Map()) }
        drive.files().list().setPageToken(files.last.getId)
        FutureStream.Nil
    }

    def getMeta(path: Path): Future[Option[EntityMeta]] =
        Future.successful(None)

    def get(path: Path): Future[Option[Entity[Array[Byte]]]] =
        Future.successful(None)

    def putContent(path: Path, content: Array[Byte]): Future[Boolean] =
        Future.successful(false)

    def delete(path: Path, recursive: Boolean): Future[Boolean] =
        Future.successful(false)

    def mkdir(path: Path, recursive: Boolean): Future[Boolean] =
        Future.successful(false)
}
