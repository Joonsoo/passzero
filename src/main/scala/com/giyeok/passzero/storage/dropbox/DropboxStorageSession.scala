package com.giyeok.passzero.storage.dropbox

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Try
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.GetMetadataErrorException
import com.dropbox.core.v2.files.ListFolderResult
import com.dropbox.core.v2.files.Metadata
import com.giyeok.passzero.storage.Entity
import com.giyeok.passzero.storage.EntityMeta
import com.giyeok.passzero.storage.EntityType
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.storage.StorageSession
import com.giyeok.passzero.utils.BytesInputStream
import com.giyeok.passzero.utils.BytesOutputStream
import com.giyeok.passzero.utils.FutureStream

class DropboxStorageSession(val profile: DropboxStorageProfile) extends StorageSession {
    private val config = DbxRequestConfig.newBuilder(profile.appName).build()
    private val client = new DbxClientV2(config, profile.accessToken)

    private implicit val ec = ExecutionContext.global

    private def metadataToEntityMeta(path: Path, metadata: Metadata): Option[EntityMeta] =
        metadata match {
            case meta: FolderMetadata =>
                Some(EntityMeta(path / meta.getName, meta.getId, EntityType.Folder, Map()))
            case meta: FileMetadata =>
                Some(EntityMeta(path / meta.getName, meta.getId, EntityType.File, Map()))
            case _ =>
                None
        }

    def list(path: Path): FutureStream[Seq[EntityMeta]] = {
        def handleResult(resultFuture: Future[ListFolderResult]): FutureStream[Seq[EntityMeta]] = {
            FutureStream.Cons(resultFuture map { result =>
                val page = result.getEntries.asScala flatMap { metadata => metadataToEntityMeta(path, metadata) }
                if (result.getHasMore) {
                    val nextPageFuture = handleResult(Future { client.files().listFolderContinue(result.getCursor) })
                    (page, nextPageFuture)
                } else {
                    (page, FutureStream.Nil)
                }
            })
        }
        handleResult(Future { client.files().listFolder(path.string) })
    }

    def getMeta(path: Path): Future[Option[EntityMeta]] = Future {
        Try(client.files().getMetadata(path.string)).toOption flatMap { metadataToEntityMeta(path, _) }
    }

    def get(path: Path): Future[Option[Entity[Array[Byte]]]] = Future {
        Try {
            val os = new BytesOutputStream(100)
            val downloader = client.files().download(path.string)
            downloader.download(os)
            Entity(os.finish())
        }.toOption
    }

    def putContent(path: Path, content: Array[Byte]): Future[Boolean] = Future {
        Try {
            client.files().upload(path.string).uploadAndFinish(new BytesInputStream(content))
        }.isSuccess
    }

    def delete(path: Path, recursive: Boolean): Future[Boolean] = Future {
        Try(client.files().delete(path.string)).isSuccess
    }

    def mkdir(path: Path, recursive: Boolean): Future[Boolean] = Future {
        Try(client.files().getMetadata(path.string)) match {
            case Failure(_: GetMetadataErrorException) =>
                // not exists이면
                client.files().createFolder(path.string)
                true
            case _ => false
        }
    }
}
