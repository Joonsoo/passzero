package com.giyeok.passzero.storage

import scala.concurrent.Future
import com.giyeok.passzero.utils.FutureStream

// TODO StorageSession이 재설정되어야 하면 Session에 변경이 일어나야 함
trait StorageSession {
    def profile: StorageProfile

    // TODO list는 paging되는 경우 정보를 얻어오는대로 전달한다
    def list(path: Path): FutureStream[Seq[EntityMeta]]

    def getMeta(path: Path): Future[Option[EntityMeta]]

    def get(path: Path): Future[Option[Entity[Array[Byte]]]]

    // putContent가 성공하면 true, 실패하면 false
    def putContent(path: Path, content: Array[Byte]): Future[Boolean]

    // delete가 성공하면 true, 실패하면 false
    def delete(path: Path, recursive: Boolean = false): Future[Boolean]

    // mkdir이 성공하면 true, 실패하면 false
    def mkdir(path: Path, recursive: Boolean = false): Future[Boolean]
}
