package com.giyeok.passzero.storage.local

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Random
import com.giyeok.passzero.StorageSessionManager
import com.giyeok.passzero.storage.Entity
import com.giyeok.passzero.storage.EntityMeta
import com.giyeok.passzero.storage.EntityType
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.storage.StorageSession
import com.giyeok.passzero.utils.ByteBuf
import com.giyeok.passzero.utils.FutureStream
import com.giyeok.passzero.utils.FutureStream.Cons

class LocalStorageSession(
        val profile: LocalStorageProfile,
        rootDirectory: File,
        manager: StorageSessionManager
) extends StorageSession {
    private implicit val ec = ExecutionContext.global

    def pathFile(path: Path): File =
        new File(rootDirectory, path.string)

    def entityTypeOf(file: File): EntityType.Value =
        if (file.isDirectory) EntityType.Folder else EntityType.File

    def list(path: Path): FutureStream[Seq[EntityMeta]] = {
        val future = Future {
            val files = Random.shuffle(pathFile(path).list().toSeq map { name =>
                val p = path / name
                EntityMeta(p, p.string, entityTypeOf(new File(p.string)), Map())
            })
            def waiter(files: Seq[EntityMeta]): (Seq[EntityMeta], FutureStream[Seq[EntityMeta]]) =
                if (files.isEmpty) (Seq(), FutureStream.empty) else {
                    val next = files.tail
                    (Seq(files.head), Cons(Future {
                        Thread.sleep(10); waiter(next)
                    }))
                }
            waiter(files)
        }
        Cons(future)
    }

    def getMeta(path: Path): Future[Option[EntityMeta]] = Future {
        val file = pathFile(path)
        if (file.exists()) Some(EntityMeta(path, path.string, entityTypeOf(file), Map())) else None
    }

    def get(path: Path): Future[Option[Entity[Array[Byte]]]] = Future {
        val f = pathFile(path)
        println("get", path.path, f.getCanonicalPath)
        if (f.exists()) {
            val is = new FileInputStream(f)
            try {
                val buf = new ByteBuf(100)
                buf.writeStream(is)
                Some(Entity(buf.finish()))
            } finally {
                is.close()
            }
        } else {
            None
        }
    }

    def putContent(path: Path, content: Array[Byte]): Future[Boolean] = Future {
        val f = pathFile(path)
        val os = new FileOutputStream(f)
        try {
            os.write(content)
            true
        } catch {
            case e: Throwable =>
                e.printStackTrace()
                false
        } finally {
            os.close()
        }
    }

    def delete(path: Path, recursive: Boolean): Future[Boolean] = Future {
        val f = pathFile(path)
        println(s"delete ${f.getCanonicalPath}")
        // f.delete()
        true
    }

    def mkdir(path: Path, recursive: Boolean): Future[Boolean] = Future {
        val f = pathFile(path)
        println(s"mkdir ${f.getCanonicalPath}")
        f.mkdir()
    }
}
