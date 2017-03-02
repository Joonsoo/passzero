package com.giyeok.passzero.storage

trait StorageSession {
    def profile: StorageProfile
    def list(path: Path): Seq[EntityMeta]
    def get(path: Path): Option[Entity[Array[Byte]]]
    def putContent(path: Path, content: Array[Byte]): Unit
    def delete(path: Path, recursive: Boolean): Boolean
    def mkdir(path: Path, recursive: Boolean): Unit
}
