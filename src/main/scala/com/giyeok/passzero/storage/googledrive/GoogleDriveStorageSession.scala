package com.giyeok.passzero.storage.googledrive

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
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
    private implicit val ec = ExecutionContext.global

    def list(path: Path): FutureStream[Seq[EntityMeta]] = {
        def filesFuture(pageTokenOpt: Option[String]): FutureStream[Seq[EntityMeta]] = {
            val pageFuture = Future {
                val list = drive.files().list().execute()
                pageTokenOpt foreach { list.setNextPageToken }
                val files = list.getFiles.iterator().asScala.toSeq
                (files map { f => EntityMeta(path / f.getName, f.getId, ???, Map()) }, Option(list.getNextPageToken))
                // drive.files().list().setPageToken(files.last.getId)
            }
            FutureStream.Cons(pageFuture map { pageNextToken =>
                val (page, nextTokenOpt) = pageNextToken
                nextTokenOpt match {
                    case Some(nextToken) =>
                        (page, filesFuture(Some(nextToken)))
                    case None =>
                        (page, FutureStream.Nil)
                }
            })
        }
        filesFuture(None)
    }

    def getMeta(path: Path): Future[Option[EntityMeta]] = Future {
        ???
    }

    def get(path: Path): Future[Option[Entity[Array[Byte]]]] = Future {
        ???
    }

    def putContent(path: Path, content: Array[Byte]): Future[Boolean] = Future {
        ???
    }

    def delete(path: Path, recursive: Boolean): Future[Boolean] = Future {
        ???
    }

    def mkdir(path: Path, recursive: Boolean): Future[Boolean] = Future {
        ???
    }
}
