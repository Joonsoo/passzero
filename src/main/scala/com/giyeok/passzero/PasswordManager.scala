package com.giyeok.passzero

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.giyeok.passzero.Password.Directory
import com.giyeok.passzero.Password.Field
import com.giyeok.passzero.Password.KeyType
import com.giyeok.passzero.Password.Sheet
import com.giyeok.passzero.Password.SheetType
import org.json4s._

class PasswordManager(session: Session) {
    private implicit val ec = ExecutionContext.global

    def directoryList(): Stream[Password.Directory] = {
        //        val listStream = session.list(session.rootPath)
        //        val y = listStream map { meta => session.getAsString(meta.path / "info") }
        //        val x = Future.traverse(listStream) { meta => session.getAsJson(meta.path / "info") }
        //        listStream flatMap { meta =>
        //            session.getAsJson(meta.path / "info") flatMap {
        //                case Some(info) =>
        //                    val x = info.content \ "name" match {
        //                        case JString(directoryName) =>
        //                            Some(Password.Directory(meta.id, directoryName))
        //                        case _ =>
        //                            // TODO 잘못된 상황
        //                            None
        //                    }
        //                    ???
        //                case None => ???
        //            }
        //            ???
        //        }
        ???
    }

    def sheetList(directory: Password.Directory): Stream[Password.Sheet] =
        ???

    def content(sheet: Password.Sheet): Seq[Password.Field] =
        ???

    def createDirectory(name: String): Option[Directory] =
        ???

    def createSheet(directory: Directory, name: String, sheetType: SheetType.Value): Option[Sheet] =
        ???

    def addField(sheet: Sheet, order: Int, key: KeyType.Value, value: String): Option[Field] =
        ???

    def removeField(field: Field): Boolean =
        ???

    def updateField(field: Field, key: KeyType.Value, value: String): Option[Field] =
        ???

    def moveField(field: Field, order: Int): Option[Field] =
        ???
}

object Password {
    object SheetType extends Enumeration {
        val Login, Unknown = Value

        val mapping = Map(
            Login -> "login"
        )
        val reverse: Map[String, SheetType.Value] =
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

    case class Directory(id: String, name: String)

    case class Sheet(directory: Directory, name: String, sheetType: SheetType.Value)

    case class Field(sheet: Sheet, order: Int, key: KeyType.Value, value: String)
}
