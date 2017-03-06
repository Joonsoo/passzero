package com.giyeok.passzero.storage.memory

import com.giyeok.passzero.storage.Entity
import com.giyeok.passzero.storage.EntityMeta
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.storage.StorageSession
import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.giyeok.passzero.utils.ByteArrayUtil._
import com.giyeok.passzero.utils.FutureStream

// 실제로 아무데도 저장하지 않는 on memory 스토리지 - 테스팅 용도
class MemoryStorageSession(val profile: MemoryStorageProfile) extends StorageSession {
    private implicit val ec = ExecutionContext.global

    val map: MutableMap[Path, (EntityMeta, Array[Byte])] =
        MutableMap[Path, (EntityMeta, Array[Byte])]()

    def list(path: Path): FutureStream[Seq[EntityMeta]] = ???

    def getMeta(path: Path): Future[Option[EntityMeta]] = ???

    def get(path: Path): Future[Option[Entity[Array[Byte]]]] =
        Future { map get path map { p => Entity(p._1, p._2) } }

    def putContent(path: Path, content: Array[Byte]): Future[Unit] = Future {
        map get path match {
            case Some((meta, _)) =>
                map(path) = (meta, content)
            case None =>
                map(path) = (EntityMeta(path, path.string, Map()), content)
        }
    }

    def delete(path: Path, recursive: Boolean): Future[Boolean] = ???

    def mkdir(path: Path, recursive: Boolean): Future[Unit] = ???

    def printHexMatrixOfFile(path: Path): Unit = {
        get(path) foreach {
            _ foreach { entity =>
                entity.content.printHexMatrix()
            }
        }
    }
}
