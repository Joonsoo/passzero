package com.giyeok.passzero.storage

case class EntityMeta(path: Path, id: String, metas: Map[String, String])

case class Entity[T](meta: EntityMeta, content: T) {
    def mapContent[U](func: T => U): Entity[U] =
        Entity(meta, func(content))
}
