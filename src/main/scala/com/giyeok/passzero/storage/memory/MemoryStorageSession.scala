package com.giyeok.passzero.storage.memory

import com.giyeok.passzero.storage.Entity
import com.giyeok.passzero.storage.EntityMeta
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.storage.StorageSession

// 실제로 아무데도 저장하지 않는 on memory 스토리지 - 테스팅 용도
class MemoryStorageSession(val profile: MemoryStorageProfile) extends StorageSession {
    override def list(path: Path): Seq[EntityMeta] = ???

    override def get(path: Path): Entity[Array[Byte]] = ???

    override def putContent(path: Path, content: Array[Byte]): Unit = ???

    override def delete(path: Path, recursive: Boolean): Boolean = ???

    override def mkdir(path: Path, recursive: Boolean): Unit = ???
}
