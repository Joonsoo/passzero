package com.giyeok.passzero.storage
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.giyeok.passzero.utils.FutureStream

class CachedStorageSession(storage: StorageSession) extends StorageSession {
    def profile: StorageProfile = storage.profile

    private val listCache = scala.collection.mutable.Map[Path, FutureStream[Seq[EntityMeta]]]()
    private val metaCache = scala.collection.mutable.Map[Path, Option[EntityMeta]]()
    private val fileCache = scala.collection.mutable.Map[Path, Option[Entity[Array[Byte]]]]()
    private implicit val ec = ExecutionContext.global

    def list(path: Path): FutureStream[Seq[EntityMeta]] =
        listCache get path match {
            case Some(cached) => cached
            case None =>
                val newFutureStream = storage.list(path)
                CachedStorageSession.this.synchronized {
                    listCache(path) = newFutureStream
                }
                newFutureStream map { list =>
                    CachedStorageSession.this.synchronized {
                        list foreach { meta =>
                            metaCache(meta.path) = Some(meta)
                        }
                    }
                }
                newFutureStream
        }

    def getMeta(path: Path): Future[Option[EntityMeta]] = {
        metaCache get path match {
            case Some(cached) => Future.successful(cached)
            case None =>
                storage.getMeta(path) map { metaOpt =>
                    CachedStorageSession.this.synchronized {
                        metaCache(path) = metaOpt
                    }
                    metaOpt
                }
        }
    }

    def get(path: Path): Future[Option[Entity[Array[Byte]]]] = {
        fileCache get path match {
            case Some(cached) => Future.successful(cached)
            case None =>
                storage.get(path) map { fileOpt =>
                    CachedStorageSession.this.synchronized {
                        fileCache(path) = fileOpt
                    }
                    fileOpt
                }
        }
    }

    def putContent(path: Path, content: Array[Byte]): Future[Boolean] = {
        CachedStorageSession.this.synchronized {
            fileCache.remove(path)
        }
        storage.putContent(path, content) map { result =>
            if (result) {
                CachedStorageSession.this.synchronized {
                    fileCache(path) = Some(Entity(content))
                }
            }
            result
        }
    }

    def delete(path: Path, recursive: Boolean): Future[Boolean] = {
        // 아직 쓰는데 없으니까 일단..
        ???
    }

    def mkdir(path: Path, recursive: Boolean): Future[Boolean] = {
        CachedStorageSession.this.synchronized {
            path.parent foreach { parent =>
                fileCache.remove(parent)
            }
        }
        storage.mkdir(path, recursive)
    }
}
