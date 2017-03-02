package com.giyeok.passzero.storage.memory

import com.giyeok.passzero.storage.Entity
import com.giyeok.passzero.storage.EntityMeta
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.storage.StorageSession
import scala.collection.mutable.{Map => MutableMap}
import com.giyeok.passzero.utils.ByteArrayUtil._

// 실제로 아무데도 저장하지 않는 on memory 스토리지 - 테스팅 용도
class MemoryStorageSession(val profile: MemoryStorageProfile) extends StorageSession {

    val map: MutableMap[Path, (EntityMeta, Array[Byte])] =
        MutableMap[Path, (EntityMeta, Array[Byte])]()

    override def list(path: Path): Seq[EntityMeta] = ???

    override def get(path: Path): Option[Entity[Array[Byte]]] =
        map get path map { p => Entity(p._1, p._2) }

    override def putContent(path: Path, content: Array[Byte]): Unit =
        map get path match {
            case Some((meta, _)) =>
                map(path) = (meta, content)
            case None =>
                map(path) = (EntityMeta(path, Map()), content)
        }

    override def delete(path: Path, recursive: Boolean): Boolean = ???

    override def mkdir(path: Path, recursive: Boolean): Unit = ???

    def printHexMatrixOfFile(path: Path): Unit = {
        get(path) foreach { entity =>
            entity.content.printHexMatrix()
        }
    }
}
