package com.giyeok.passzero.storage

// TODO StorageSession이 재설정되어야 하면 Session에 변경이 일어나야 함
trait StorageSession {
    def profile: StorageProfile
    def list(path: Path): Stream[EntityMeta]
    def get(path: Path): Option[Entity[Array[Byte]]]
    def putContent(path: Path, content: Array[Byte]): Unit
    def delete(path: Path, recursive: Boolean): Boolean
    def mkdir(path: Path, recursive: Boolean): Unit
}
