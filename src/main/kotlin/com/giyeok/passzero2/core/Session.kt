package com.giyeok.passzero2.core

import com.fasterxml.jackson.databind.JsonNode
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.time.LocalDateTime
import java.util.*


interface Session {
    enum class SheetType(val code: String) {
        Login("login"), Note("note"), Other("other");

        companion object {
            fun ofCode(code: String): SheetType =
                    when (code) {
                        Login.code -> Login
                        Note.code -> Note
                        else -> Other
                    }
        }
    }

    enum class FieldType(val code: String) {
        Username("username"), Password("password"), Website("website"), Note("note"), Other("other");

        companion object {
            fun ofCode(code: String): FieldType =
                    when (code) {
                        Username.code -> Username
                        Password.code -> Password
                        Website.code -> Website
                        Note.code -> Note
                        else -> Other
                    }
        }
    }

    data class Sheet(val id: String, val meta: SheetMeta, val detail: SheetDetail)

    /**
     * @property version: greater version means later version. Usually timestamp
     */
    data class SheetMeta(
            val version: Long,
            val sheetType: SheetType,
            val name: String,
            val tags: List<String>,
            val createdTime: Date,
            val modifiedTime: Date) {
        fun toJson(): JsonNode = TODO()
    }

    /**
     * @property version: greater version means later version. Usually timestamp
     * meta.version and detail.version are totally independent, which means two entities managed independently
     */
    data class SheetDetail(val version: Long, val fields: List<SheetField>) {
        fun toJson(): JsonNode = TODO()
    }

    data class SheetField(val type: FieldType, val value: String)

    data class SheetCache(val id: String, val meta: SheetMeta, val detail: SheetDetail?)

    data class FirstInfo(
            val cachedSheets: List<SheetCache>,
            val cachedTags: List<String>)

    class Dump(val sheets: List<Sheet>)

    fun start(): Completable
    fun firstInfo(): Single<FirstInfo>
    fun locate(sheetId: String): Single<String>
    fun sheetMetas(): Observable<Pair<String, SheetMeta>>
    fun sheet(sheetId: String): Single<Sheet>
    fun createSheet(meta: SheetMeta, detail: SheetDetail): Single<Sheet>
    fun updateSheet(sheetId: String, newMeta: SheetMeta, newDetail: SheetDetail): Single<Sheet>
    fun deleteSheet(sheetId: String): Completable
    fun clearAllCaches(): Completable
    fun clearSheetCache(sheetId: String): Single<Sheet>
    fun dump(): Single<Dump>
    fun import(dump: Dump): Observable<Sheet>
}
