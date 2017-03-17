package com.giyeok.passzero.storage.dropbox

import java.io.ByteArrayInputStream
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.GetMetadataErrorException
import com.dropbox.core.v2.files.ListFolderResult
import com.dropbox.core.v2.files.Metadata
import com.dropbox.core.v2.files.WriteMode
import com.giyeok.passzero.storage.Cacheable
import com.giyeok.passzero.storage.Entity
import com.giyeok.passzero.storage.EntityMeta
import com.giyeok.passzero.storage.EntityType
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.storage.StorageSession
import com.giyeok.passzero.utils.BytesOutputStream
import com.giyeok.passzero.utils.FutureStream

class DropboxStorageSession(val profile: DropboxStorageProfile) extends StorageSession with Cacheable {
    private val config = DbxRequestConfig.newBuilder(profile.appName).build()
    private val _client = new DbxClientV2(config, profile.accessToken)
    private val root = Path(profile.rootPath)
    private lazy val files = _client.files()

    private implicit val ec = ExecutionContext.global

    private def metadataToEntityMeta(path0: Path, metadata: Metadata): Option[EntityMeta] =
        metadata match {
            case meta: FolderMetadata =>
                Some(EntityMeta(path0 / meta.getName, meta.getId, EntityType.Folder, Map()))
            case meta: FileMetadata =>
                Some(EntityMeta(path0 / meta.getName, meta.getId, EntityType.File, Map()))
            case _ =>
                None
        }

    def list(path0: Path): FutureStream[Seq[EntityMeta]] = {
        val path = root / path0
        def handleResult(resultFuture: Future[ListFolderResult]): FutureStream[Seq[EntityMeta]] = {
            val consFuture: Future[(Seq[EntityMeta], FutureStream[Seq[EntityMeta]])] = resultFuture map { result =>
                val page = result.getEntries.asScala.toList flatMap { metadata =>
                    metadataToEntityMeta(path0, metadata)
                }
                if (result.getHasMore) {
                    val listFuture = Future {
                        files.listFolderContinue(result.getCursor)
                    }
                    (page, handleResult(listFuture))
                } else {
                    (page, FutureStream.Nil)
                }
            }
            FutureStream.Cons(consFuture)
        }
        val listFuture = Future {
            files.listFolder(path.string)
        }
        handleResult(listFuture)
    }

    def getMeta(path0: Path): Future[Option[EntityMeta]] =
        meta(path0) {
            Future {
                val path = root / path0
                val t = Try(files.getMetadata(path.string))
                println("getMeta", path.string, t)
                t.toOption flatMap { metadataToEntityMeta(path0, _) }
            }
        }

    def get(path0: Path): Future[Option[Entity[Array[Byte]]]] =
        content(path0) {
            Future {
                val path = root / path0
                Try {
                    val os = new BytesOutputStream(100)
                    println("get", path.string)
                    val downloader = files.download(path.string)
                    downloader.download(os)
                    println(s"done ${path.string} - ${os.buf.length}")
                    Entity(os.finish())
                } match {
                    case Success(result) => Some(result)
                    case Failure(reason) =>
                        reason.printStackTrace()
                        None
                }
            }
        }

    def putContent(path0: Path, content: Array[Byte]): Future[Boolean] = {
        Future {
            val path = root / path0
            Try {
                println("put", path.string)
                val uploader = files.uploadBuilder(path.string).withMode(WriteMode.OVERWRITE).start()
                try {
                    uploader.uploadAndFinish(new ByteArrayInputStream(content))
                } finally {
                    uploader.close()
                }
            } match {
                case Success(result) =>
                    cacheContent(path0, Some(Entity(content)))
                    true
                case Failure(reason) =>
                    reason.printStackTrace()
                    false
            }
        }
    }

    def delete(path0: Path, recursive: Boolean): Future[Boolean] = Future {
        deleteCache(path0)
        val path = root / path0
        Try(files.delete(path.string)).isSuccess
    }

    def mkdir(path0: Path, recursive: Boolean): Future[Boolean] = Future {
        val path = root / path0
        val t = Try(files.getMetadata(path.string))
        println("mkdir", path.string, t)
        t match {
            case Failure(_: GetMetadataErrorException) =>
                // not exists이면
                files.createFolder(path.string)
                true
            case _ => false
        }
    }
}
