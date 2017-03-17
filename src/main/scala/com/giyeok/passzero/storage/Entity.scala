package com.giyeok.passzero.storage

object EntityType extends Enumeration {
    val File, Folder = Value
}
case class EntityMeta(path: Path, id: String, entityType: EntityType.Value, metas: Map[String, String]) {
    def isDirectory: Boolean =
        entityType == EntityType.Folder
}

case class Entity[T](content: T) {
    def mapContent[U](func: T => U): Entity[U] =
        Entity(func(content))
}
