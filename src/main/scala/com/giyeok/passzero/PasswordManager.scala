package com.giyeok.passzero

class PasswordManager(session: Session) {
    def directoryList(): Seq[Password.Directory] =
        ???

    def sheetList(directory: Password.Directory): Seq[Password.Item] =
        ???

    def content(item: Password.Item): Seq[Password.ItemField] =
        ???
}

object Password {
    object ItemType extends Enumeration {
        val Login, Unknown = Value

        val mapping = Map(
            Login -> "login"
        )
        val reverse: Map[String, ItemType.Value] =
            mapping map { x => x._2 -> x._1 }
    }
    object KeyType extends Enumeration {
        val Username, Password, Website, Note, Unknown = Value

        val mapping = Map(
            Username -> "username",
            Password -> "password",
            Website -> "website",
            Note -> "note"
        )
        val reverse: Map[String, KeyType.Value] =
            mapping map { x => x._2 -> x._1 }
    }

    case class Directory(name: String)

    case class Item(directory: Directory, name: String, itemType: ItemType.Value)

    case class ItemField(item: Item, order: Int, key: KeyType.Value, value: String)
}
