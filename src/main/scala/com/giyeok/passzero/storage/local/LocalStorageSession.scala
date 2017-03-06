package com.giyeok.passzero.storage.local

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.giyeok.passzero.StorageSessionManager
import com.giyeok.passzero.storage.Entity
import com.giyeok.passzero.storage.EntityMeta
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.storage.StorageSession
import com.giyeok.passzero.utils.ByteBuf

class LocalStorageSession(
        val profile: LocalStorageProfile,
        rootDirectory: File,
        manager: StorageSessionManager
) extends StorageSession {
    private implicit val ec = ExecutionContext.global

    def pathFile(path: Path): File =
        new File(rootDirectory, path.string)

    def list(path: Path): Stream[Future[Seq[EntityMeta]]] = {
        //        pathFile(path).list().toStream map { name =>
        //            val p = path / name
        //            EntityMeta(p, p.string, Map())
        //        }
        ???
    }

    def getMeta(path: Path): Future[Option[EntityMeta]] = ???

    def get(path: Path): Future[Option[Entity[Array[Byte]]]] = Future {
        val f = pathFile(path)
        if (f.exists()) {
            val is = new FileInputStream(f)
            try {
                val buf = new ByteBuf(100)
                buf.writeStream(is)
                Some(Entity(EntityMeta(path, path.string, Map()), buf.finish()))
            } finally {
                is.close()
            }
        } else {
            None
        }
    }

    def putContent(path: Path, content: Array[Byte]): Future[Unit] = Future {
        val f = pathFile(path)
        val os = new FileOutputStream(f)
        try {
            os.write(content)
        } finally {
            os.close()
        }
    }

    def delete(path: Path, recursive: Boolean): Future[Boolean] = ???

    def mkdir(path: Path, recursive: Boolean): Future[Unit] = ???
}
