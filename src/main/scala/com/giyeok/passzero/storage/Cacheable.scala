package com.giyeok.passzero.storage

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait Cacheable {
    private implicit val ec = ExecutionContext.global

    // TODO list cache
    private var metaCache = Map[Path, Option[EntityMeta]]()
    private var contentCache = Map[Path, Option[Entity[Array[Byte]]]]()

    def cacheMeta(path: Path, meta: Option[EntityMeta]): Unit = this.synchronized {
        metaCache += (path -> meta)
    }
    def cacheContent(path: Path, content: Option[Entity[Array[Byte]]]): Unit = this.synchronized {
        contentCache += (path -> content)
    }

    def cachedMeta(path: Path): Option[Option[EntityMeta]] = this.synchronized {
        metaCache get path
    }
    def cachedContent(path: Path): Option[Option[Entity[Array[Byte]]]] = this.synchronized {
        contentCache get path
    }

    def meta(path: Path)(orElseFuture: => Future[Option[EntityMeta]]): Future[Option[EntityMeta]] = this.synchronized {
        cachedMeta(path) match {
            case Some(meta) => Future.successful(meta)
            case None =>
                orElseFuture map { meta =>
                    cacheMeta(path, meta)
                    meta
                }
        }
    }
    def content(path: Path)(orElseFuture: => Future[Option[Entity[Array[Byte]]]]): Future[Option[Entity[Array[Byte]]]] = this.synchronized {
        cachedContent(path) match {
            case Some(content) => Future.successful(content)
            case None =>
                orElseFuture map { content =>
                    cacheContent(path, content)
                    content
                }
        }
    }

    def deleteCache(path: Path): Unit = this.synchronized {
        metaCache -= path
        contentCache -= path
    }
}
