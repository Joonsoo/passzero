package com.giyeok.passzero

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Random
import com.giyeok.passzero.Password.Directory
import com.giyeok.passzero.Password.Field
import com.giyeok.passzero.Password.KeyType
import com.giyeok.passzero.Password.Sheet
import com.giyeok.passzero.Password.SheetDetail
import com.giyeok.passzero.Password.SheetType
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.utils.FutureStream
import org.json4s.JsonDSL._
import org.json4s._

class PasswordManager(session: Session) {
    private implicit val ec = ExecutionContext.global

    def directoryPath(directory: Directory): Path =
        session.rootPath / directory.id
    def sheetPath(sheet: Sheet): Path =
        directoryPath(sheet.directory) / sheet.id

    def newId(): String =
        s"${System.currentTimeMillis()}_${Math.abs(Random.nextLong())}"

    object directory {
        def infoJsonOf(directory: Directory): JValue =
            "name" -> directory.name

        private def directryOf(id: String, infoJson: JValue): Option[Directory] =
            infoJson \ "name" match {
                case JString(directoryName) =>
                    Some(Password.Directory(id, directoryName))
                case _ =>
                    // 잘못된 상황 - json 내용 오류?
                    None
            }

        def directoryList(): FutureStream[Seq[Password.Directory]] = {
            session.list(session.rootPath) map1 { page =>
                Future.sequence(page map { meta =>
                    session.getAsJson(meta.path / "info") map {
                        case Some(info) =>
                            directryOf(meta.path.name, info.content)
                        case None =>
                            // 잘못된 상황 - 파싱 실패?
                            None
                    }
                }) map { _.flatten }
            }
        }

        def createDirectory(name: String): Future[Option[Directory]] = {
            val id: String = newId()
            val directory = Directory(id, name)
            val path = directoryPath(directory)
            session.mkdir(path) flatMap {
                case true =>
                    session.putJson(path / "info", infoJsonOf(directory)) map {
                        case true => Some(directory)
                        case false => None
                    }
                case false =>
                    Future.successful(None)
            }
        }

        def updateDirectory(directory: Directory, name: String): Future[Option[Directory]] = {
            val path = directoryPath(directory)
            val newDirectory = Directory(directory.id, name)
            session.putJson(path / "info", infoJsonOf(newDirectory)) map {
                case true => Some(newDirectory)
                case false => None
            }
        }
    }

    object sheet {
        def infoJsonOf(sheet: Sheet): JValue =
            ("name" -> sheet.name) ~ ("type" -> SheetType.mapping(sheet.sheetType))

        private def sheetOf(directory: Directory, id: String, infoJson: JValue): Option[Sheet] =
            (infoJson \ "name", infoJson \ "type") match {
                case (JString(sheetName), JString(Password.SheetType(sheetType))) =>
                    Some(Password.Sheet(directory, id, sheetName, sheetType))
                case _ =>
                    // 잘못된 상황
                    None
            }

        def sheetList(directory: Password.Directory): FutureStream[Seq[Password.Sheet]] = {
            session.list(directoryPath(directory)) map1 { page =>
                Future.sequence(page map { meta =>
                    session.getAsJson(meta.path / "info") map {
                        case Some(info) =>
                            sheetOf(directory, meta.path.name, info.content)
                        case None =>
                            // 잘못된 상황 - 파싱 실패?
                            None
                    }
                }) map { _.flatten }
            }
        }

        def createSheet(directory: Directory, name: String, sheetType: SheetType.Value): Future[Option[Sheet]] = {
            val id: String = newId()
            val sheet = Sheet(directory, id, name, sheetType)
            val path = sheetPath(sheet)
            session.mkdir(path) flatMap {
                case true =>
                    session.putJson(path / "info", infoJsonOf(sheet)) map {
                        case true => Some(sheet)
                        case false => None
                    }
                case false => Future.successful(None)
            }
        }

        def updateSheet(sheet: Sheet, name: String, sheetType: SheetType.Value): Future[Option[Sheet]] = {
            val newSheet = Sheet(sheet.directory, sheet.id, name, sheetType)
            val path = sheetPath(sheet)
            session.putJson(path / "info", infoJsonOf(newSheet)) map {
                case true => Some(newSheet)
                case false => None
            }
        }
    }

    object sheetDetail {
        def jsonOf(detail: SheetDetail): JValue =
            detail.fields map { field =>
                ("key" -> KeyType.mapping(field.key)) ~ ("v" -> field.value)
            }

        private def sheetDetailOf(sheet: Sheet, infoJson: JValue): Option[SheetDetail] = {
            val fields: Seq[Option[Field]] = infoJson match {
                case JArray(seq) =>
                    seq map {
                        case obj: JObject =>
                            ???
                        case _ => None
                    }
                case _ => Seq(None)
            }
            if (fields contains None) None else Some(SheetDetail(sheet, fields.flatten))
        }

        def sheetDetail(sheet: Password.Sheet): Future[Option[SheetDetail]] =
            session.getAsJson(sheetPath(sheet) / "detail") map {
                case Some(entity) => sheetDetailOf(sheet, entity.content)
                case None => None
            }

        def putSheetDetail(sheet: Password.Sheet, fields: Seq[Field]): Future[Option[SheetDetail]] = {
            val path = sheetPath(sheet)
            val detail = SheetDetail(sheet, fields)
            session.putJson(path / "detail", jsonOf(detail)) map {
                case true => Some(detail)
                case false => None
            }
        }
    }
}

object Password {
    case class Directory(id: String, name: String)

    object SheetType extends Enumeration {
        val Login, Note, Unknown = Value

        val mapping = Map(
            Login -> "login",
            Note -> "note"
        )
        val reverse =
            mapping map { x => x._2 -> x._1 }

        def of(name: String): SheetType.Value = reverse(name)
        def unapply(name: String): Option[SheetType.Value] = reverse get name
    }
    case class Sheet(directory: Directory, id: String, name: String, sheetType: SheetType.Value) {
        def updateDirectory(newDirectory: Directory): Sheet = Sheet(newDirectory, id, name, sheetType)
    }

    object KeyType extends Enumeration {
        val Username, Password, Website, Note, Unknown = Value

        val mapping = Map(
            Username -> "username",
            Password -> "password",
            Website -> "website",
            Note -> "note"
        )
        val reverse =
            mapping map { x => x._2 -> x._1 }

        def of(name: String): KeyType.Value = reverse(name)
        def unapply(name: String): Option[KeyType.Value] = reverse get name
    }
    case class Field(sheet: Sheet, key: KeyType.Value, value: String)
    case class SheetDetail(sheet: Sheet, fields: Seq[Field])
}
