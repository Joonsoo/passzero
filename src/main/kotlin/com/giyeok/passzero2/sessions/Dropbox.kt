package com.giyeok.passzero2.sessions

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.giyeok.passzero2.core.Crypto
import com.giyeok.passzero2.core.Session
import com.giyeok.passzero2.core.SessionProfile
import com.giyeok.passzero2.util.mapPropagatingError
import com.giyeok.passzero2.util.readBytesFully
import com.giyeok.passzero2.util.toHexString
import io.reactivex.*
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.io.*
import java.util.*

class DropboxProfile(val appName: String, val accessToken: String, val appRootPath: String) : SessionProfile {
    companion object : SessionProfile.Loader {
        override val name = "dropbox"

        override fun fromBytes(bytes: ByteArray): DropboxProfile {
            val stream = DataInputStream(ByteArrayInputStream(bytes))

            val profileVersion = stream.readBytesFully(2)
            if (!profileVersion.contentEquals(byteArrayOf(0, 1))) {
                throw Exception("Unsupported dropbox version: ${profileVersion.toHexString()}, ${bytes.toHexString()}")
            }

            try {
                val appNameLength = stream.readInt()
                val appName = String(stream.readBytesFully(appNameLength))

                val accessTokenLength = stream.readInt()
                val accessToken = String(stream.readBytesFully(accessTokenLength))

                val rootPathLength = stream.readInt()
                val rootPath = String(stream.readBytesFully(rootPathLength))

                return DropboxProfile(appName, accessToken, rootPath)
            } catch (eof: EOFException) {
                throw Exception("EOF Exception: ${bytes.toHexString()}")
            }
        }
    }

    override val loader: SessionProfile.Loader = Companion

    override fun createSession(root: String, secretKey: ByteArray): DropboxSession =
            DropboxSession(this, root, secretKey, Schedulers.computation())

    override fun contentEquals(other: SessionProfile): Boolean =
            when (other) {
                is DropboxProfile ->
                    this.appName == other.appName &&
                            this.accessToken == other.accessToken &&
                            this.appRootPath == other.appRootPath
                else -> false
            }

    override fun toBytes(): ByteArray {
        val arrayStream = ByteArrayOutputStream()
        val stream = DataOutputStream(arrayStream)

        stream.write(0)
        stream.write(1)

        val appNameBytes = appName.toByteArray()
        stream.writeInt(appNameBytes.size)
        stream.write(appNameBytes)

        val accessTokenBytes = accessToken.toByteArray()
        stream.writeInt(accessTokenBytes.size)
        stream.write(accessTokenBytes)

        val rootPathBytes = appRootPath.toByteArray()
        stream.writeInt(rootPathBytes.size)
        stream.write(rootPathBytes)

        return arrayStream.toByteArray()
    }
}

// 여기서 Session은 저장소에 저장되어 있는 캐시를 없애기 위한 메소드같은 것들은 제공하지만, 이 클래스가 캐시를 관리하는 건 아님
// 캐시는 PasswordListUi에서 관리
class DropboxSession(
        private val profile: DropboxProfile,
        private val root: String,
        private val secretKey: ByteArray,
        private val scheduler: Scheduler) : Session {
    private lateinit var client: DbxClientV2

    companion object {
        private val objectMapper = ObjectMapper()

        fun jsonToSheetMeta(jsonText: String): Session.SheetMeta {
            val json = objectMapper.readTree(jsonText)
            val metaVersion = json["version"]?.asLong() ?: 0
            val sheetName = json["name"]?.asText() ?: "Unnamed"
            val sheetType = Session.SheetType.ofCode(json["type"]?.asText() ?: "")
            val tags = if (json["tags"]?.isArray != true) listOf() else json["tags"].map { it.asText() }
            val createdTime = Date(json["created"]?.asLong() ?: 0)
            val modifiedTime = Date(json["modified"]?.asLong() ?: 0)
            return Session.SheetMeta(metaVersion, sheetType, sheetName, tags, createdTime, modifiedTime)
        }

        fun jsonToSheetDetail(jsonText: String): Session.SheetDetail {
            val json = objectMapper.readTree(jsonText)
            if (!json.isArray) {
                TODO()
            }
            val fields = json.map { field ->
                val key = Session.FieldType.ofCode(field["key"]?.asText() ?: "")
                val value = field["v"]?.asText() ?: ""
                Session.SheetField(key, value)
            }
            return Session.SheetDetail(0, fields)
        }
    }

    class DropboxLookupError(val path: String, val value: String) : Exception("Dropbox lookup error: $path, $value")

    override fun start(): Completable = Completable.create {
        client = DbxClientV2(DbxRequestConfig.newBuilder(profile.appName).build(), profile.accessToken)
        it.onComplete()
    }.subscribeOn(scheduler)

    override fun firstInfo(): Single<Session.FirstInfo> {
        // 캐시되어 있는 정보 반환. 사용하는 쪽에서는 firstInfo()와 sheetMetas()를 동시에 호출해서 알게 되는 정보를 최대한 사용
        return Single.just(Session.FirstInfo(listOf(), listOf()))
    }

    private fun rootPath(): String = "${profile.appRootPath}/$root"
    private fun pathOf(sheetId: String): String = "${rootPath()}/$sheetId"

    override fun locate(sheetId: String): Single<String> =
            Single.just("${pathOf(sheetId)}/{info|detail}")

    override fun sheetMetas(): Observable<Pair<String, Session.SheetMeta>> = listFolder(rootPath())
            .doOnError {
                println("Error while listing folder: ${rootPath()}: $it")
            }
            .flatMap({ entry ->
                if (entry is FolderMetadata) {
                    Observable.create<Pair<String, Session.SheetMeta>> { emitter ->
                        println("Sending request to fetch ${entry.pathLower}/info")
                        loadString("${entry.pathLower}/info")
                                .subscribeOn(scheduler)
                                .subscribe(
                                        { info ->
                                            println("Successfully fetched ${entry.pathLower}/info")
                                            emitter.onNext(Pair(entry.name, jsonToSheetMeta(info)))
                                            emitter.onComplete()
                                        },
                                        { error ->
                                            println("Error while loading info file: ${entry.pathLower}/info: $error")
                                            emitter.onError(error)
                                        })
                    }.subscribeOn(scheduler)
                } else Observable.empty()
            }, true)

    override fun sheet(sheetId: String): Single<Session.Sheet> =
    // TODO zip with error propagating?
            Single.zip(
                    loadString("${pathOf(sheetId)}/info"),
                    loadString("${pathOf(sheetId)}/detail"),
                    BiFunction { info: String, detail: String ->
                        Session.Sheet(sheetId, jsonToSheetMeta(info), jsonToSheetDetail(detail))
                    }).subscribeOn(scheduler)

    override fun createSheet(meta: Session.SheetMeta, detail: Session.SheetDetail): Single<Session.Sheet> {
        TODO()
    }

    override fun updateSheet(sheetId: String, newMeta: Session.SheetMeta, newDetail: Session.SheetDetail): Single<Session.Sheet> {
        TODO()
    }

    override fun deleteSheet(sheetId: String): Completable {
        TODO()
    }

    override fun clearAllCaches(): Completable {
        TODO()
    }

    override fun clearSheetCache(sheetId: String): Single<Session.Sheet> {
        TODO()
    }

    override fun dump(): Single<Session.Dump> {
        TODO()
    }

    override fun import(dump: Session.Dump): Observable<Session.Sheet> {
        TODO()
    }

    fun listFolder(path: String): Observable<Metadata> = Observable.create<Metadata> { emitter ->
        try {
            var result = client.files().listFolder(path)
            result.entries.forEach { entry -> emitter.onNext(entry) }
            while (result.hasMore) {
                println("Next page: ${result.cursor}")
                result = client.files().listFolderContinue(result.cursor)
                result.entries.forEach { entry -> emitter.onNext(entry) }
            }
            emitter.onComplete()
            println("listFolder.onComplete'd")
        } catch (e: Exception) {
            e.printStackTrace()
            emitter.onError(e)
        }
    }.subscribeOn(scheduler)

    fun mkdir(path: String): Completable = Completable.create { emitter ->
        client.files().createFolder(path)
        emitter.onComplete()
    }.subscribeOn(scheduler)

    private fun saveRaw(path: String, content: ByteArray): Completable = Completable.create { emitter ->
        client.files().uploadBuilder(path).withMode(WriteMode.OVERWRITE).start()
                .uploadAndFinish(ByteArrayInputStream(content))
        emitter.onComplete()
    }.subscribeOn(scheduler)

    fun save(path: String, content: ByteArray): Completable = Completable.create { emitter ->
        val iv = Crypto.AES256CBC.InitVec.generate()
        // TODO 이거 조금 예쁘게 쓸 수 없나?
        saveRaw(path, iv + Crypto.AES256CBC.encode(content, secretKey, iv)).subscribe { emitter.onComplete() }
    }.subscribeOn(scheduler)

    fun saveString(path: String, content: String): Completable = save(path, content.toByteArray())

    private fun loadRaw(path: String): Single<ByteArray> = Single.create { emitter: SingleEmitter<ByteArray> ->
        val os = ByteArrayOutputStream()
        try {
            client.files().download(path)
                    .download(os)
            emitter.onSuccess(os.toByteArray())
        } catch (e: DownloadErrorException) {
            emitter.onError(DropboxLookupError(path, e.errorValue.toString()))
        } catch (e: Throwable) {
            emitter.onError(e)
        }
    }.subscribeOn(scheduler)

    fun load(path: String): Single<ByteArray> = loadRaw(path)
            .mapPropagatingError { byteArray ->
                val iv = byteArray.sliceArray(0 until Crypto.AES256CBC.InitVec.size)
                val encoded = byteArray.sliceArray(Crypto.AES256CBC.InitVec.size until byteArray.size)
                Crypto.AES256CBC.decode(encoded, secretKey, iv)
            }
            .subscribeOn(scheduler)

    fun loadString(path: String): Single<String> =
            load(path).mapPropagatingError { String(it) }
}
