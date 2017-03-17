package com.giyeok.passzero

import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Random
import com.giyeok.passzero.Password.DirectoryId
import com.giyeok.passzero.Password.DirectoryInfo
import com.giyeok.passzero.Password.Field
import com.giyeok.passzero.Password.KeyType
import com.giyeok.passzero.Password.SheetDetail
import com.giyeok.passzero.Password.SheetId
import com.giyeok.passzero.Password.SheetInfo
import com.giyeok.passzero.Password.SheetType
import com.giyeok.passzero.Password.UserConfig
import com.giyeok.passzero.storage.EntityType
import com.giyeok.passzero.storage.Path
import com.giyeok.passzero.utils.FutureStream
import org.json4s.JsonDSL._
import org.json4s._

class PasswordManager(session: Session) {
    private implicit val ec = ExecutionContext.global

    def directoryPath(directoryId: DirectoryId): Path =
        session.rootPath / directoryId.id
    def sheetPath(sheetId: SheetId): Path =
        directoryPath(sheetId.directoryId) / sheetId.id

    def newId(): String =
        s"${System.currentTimeMillis()}_${Math.abs(Random.nextLong())}"

    object userConfig {
        val configPath: Path = session.rootPath / "config"

        def jsonOf(config: UserConfig): JObject =
            "defaultDirectory" -> (config.defaultDirectory map { _.id })

        def get(): Future[Option[UserConfig]] =
            session.getAsJson(configPath) map {
                case Some(entity) =>
                    val defaultDirectory = entity.content \ "defaultDirectory" match {
                        case JString(directoryId) => Some(DirectoryId(directoryId))
                        case _ => None
                    }
                    Some(UserConfig(defaultDirectory))
                case None => None
            }

        def put(config: UserConfig): Future[Boolean] =
            session.putJson(configPath, jsonOf(config))
    }

    object directory {
        def infoJsonOf(directoryInfo: DirectoryInfo): JValue =
            "name" -> directoryInfo.name

        private def directoryInfoOf(infoJson: JValue): Option[DirectoryInfo] =
            infoJson \ "name" match {
                case JString(directoryName) =>
                    Some(DirectoryInfo(directoryName))
                case _ =>
                    // 잘못된 상황 - json 내용 오류?
                    None
            }

        def get(id: DirectoryId): Future[Option[DirectoryInfo]] = {
            session.getAsJson(directoryPath(id) / "info") map {
                case Some(info) =>
                    directoryInfoOf(info.content)
                case None =>
                    None
            }
        }

        def directoryList(): FutureStream[Seq[(DirectoryId, DirectoryInfo)]] = {
            session.list(session.rootPath) map1 { page =>
                val s = page filter { _.entityType == EntityType.Folder } map { meta =>
                    session.getAsJson(meta.path / "info") map {
                        case Some(entity) =>
                            directoryInfoOf(entity.content) map { info => (DirectoryId(meta.path.name), info) }
                        case None =>
                            // 잘못된 상황 - 파싱 실패?
                            None
                    }
                }
                Future.sequence(s) map { _.flatten }
            }
        }

        def createDirectory(name: String): Future[Option[(DirectoryId, DirectoryInfo)]] = {
            val id = DirectoryId(newId())
            val info = DirectoryInfo(name)
            val path = directoryPath(id)
            session.mkdir(path) flatMap {
                case true =>
                    session.putJson(path / "info", infoJsonOf(info)) map {
                        case true => Some((id, info))
                        case false => None
                    }
                case false =>
                    Future.successful(None)
            }
        }

        def updateDirectory(directoryId: DirectoryId, name: String): Future[Option[DirectoryInfo]] = {
            val path = directoryPath(directoryId)
            val newDirectory = DirectoryInfo(name)
            session.putJson(path / "info", infoJsonOf(newDirectory)) map {
                case true => Some(newDirectory)
                case false => None
            }
        }
    }

    object sheet {
        def infoJsonOf(sheet: SheetInfo): JValue =
            ("name" -> sheet.name) ~ ("type" -> SheetType.mapping(sheet.sheetType))

        private def sheetOf(id: SheetId, infoJson: JValue): Option[SheetInfo] =
            (infoJson \ "name", infoJson \ "type") match {
                case (JString(sheetName), JString(Password.SheetType(sheetType))) =>
                    Some(SheetInfo(sheetName, sheetType))
                case _ =>
                    // 잘못된 상황
                    None
            }

        def get(sheetId: SheetId): Future[Option[SheetInfo]] = {
            session.getAsJson(sheetPath(sheetId) / "info") map {
                case Some(entity) =>
                    sheetOf(sheetId, entity.content)
                case None => None
            }
        }

        def sheetList(directoryId: DirectoryId): FutureStream[Seq[(SheetId, SheetInfo)]] = {
            session.list(directoryPath(directoryId)) map1 { page =>
                val s = page filter { _.entityType == EntityType.Folder } map { meta =>
                    session.getAsJson(meta.path / "info") map {
                        case Some(entity) =>
                            val id = SheetId(directoryId, meta.path.name)
                            sheetOf(id, entity.content) map { (id, _) }
                        case None =>
                            // 잘못된 상황 - 파싱 실패?
                            None
                    }
                }
                Future.sequence(s) map { _.flatten }
            }
        }

        def createSheet(directoryId: DirectoryId, name: String, sheetType: SheetType.Value): Future[Option[(SheetId, SheetInfo)]] = {
            val id = SheetId(directoryId, newId())
            val info = SheetInfo(name, sheetType)
            val path = sheetPath(id)
            session.mkdir(path) flatMap {
                case true =>
                    session.putJson(path / "info", infoJsonOf(info)) map {
                        case true => Some((id, info))
                        case false => None
                    }
                case false => Future.successful(None)
            }
        }

        def updateSheet(sheetId: SheetId, name: String, sheetType: SheetType.Value): Future[Option[SheetInfo]] = {
            val newSheet = SheetInfo(name, sheetType)
            session.putJson(sheetPath(sheetId) / "info", infoJsonOf(newSheet)) map {
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

        private def sheetDetailOf(sheetId: SheetId, infoJson: JValue): Option[SheetDetail] = {
            val fields: Seq[Field] = infoJson match {
                case JArray(seq) =>
                    seq flatMap {
                        case obj: JObject =>
                            Some(Field(KeyType.reverse((obj \ "key").asInstanceOf[JString].s), (obj \ "v").asInstanceOf[JString].s))
                        case _ => None
                    }
                case _ => Seq()
            }
            Some(SheetDetail(fields))
        }

        def get(sheetId: SheetId): Future[Option[SheetDetail]] =
            session.getAsJson(sheetPath(sheetId) / "detail") map {
                case Some(entity) => sheetDetailOf(sheetId, entity.content)
                case None => None
            }

        def putSheetDetail(sheetId: SheetId, fields: Seq[Field]): Future[Option[SheetDetail]] = {
            val detail = SheetDetail(fields)
            session.putJson(sheetPath(sheetId) / "detail", jsonOf(detail)) map {
                case true => Some(detail)
                case false => None
            }
        }
    }
}

object Password {
    object SheetType extends Enumeration {
        val Login, Note, Unknown = Value

        val mapping = ListMap(
            Login -> "login",
            Note -> "note"
        )
        val reverse: ListMap[String, SheetType.Value] =
            mapping map { x => x._2 -> x._1 }

        def of(name: String): SheetType.Value = reverse(name)
        def unapply(name: String): Option[SheetType.Value] = reverse get name
    }

    object KeyType extends Enumeration {
        val Username, Password, Website, Note, Unknown = Value

        val mapping = ListMap(
            Username -> "username",
            Password -> "password",
            Website -> "website",
            Note -> "note"
        )
        val reverse: ListMap[String, KeyType.Value] =
            mapping map { x => x._2 -> x._1 }

        def of(name: String): KeyType.Value = reverse(name)
        def unapply(name: String): Option[KeyType.Value] = reverse get name
    }

    case class UserConfig(defaultDirectory: Option[DirectoryId])

    case class DirectoryId(id: String)
    case class DirectoryInfo(name: String)

    case class SheetId(directoryId: DirectoryId, id: String)
    case class SheetInfo(name: String, sheetType: SheetType.Value)

    case class Field(key: KeyType.Value, value: String)
    case class SheetDetail(fields: Seq[Field])
}
