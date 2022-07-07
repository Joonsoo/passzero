package com.giyeok.passzero2.gui

import com.giyeok.passzero2.core.await
import com.giyeok.passzero2.core.storage.DropboxClientImpl
import com.giyeok.passzero2.core.storage.DropboxToken
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

fun main() {
  val okHttpClient = OkHttpClient()
  val gson =
    GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

//  val expiredAccessToken =
//    "sl.BK6JJMFNh6MHZqcI6UvGO8whJF0hd6h9rqPvXtU_eYVe8FChqPUf4-DBAJK25GkBurMYrCmfdC-inSWI9Z64qqJO0svwqFFH02dpmlV_tqO-xwQU7iQFHpi0z_VurfsHOaLlo8Vt"
//
//  data class TokenReq(
//    val grantType: String = "refresh_token",
//    val clientId: String = "hgndvjtq97o4jls",
//    val refreshToken: String = "gT3MoA_9K9MAAAAAAAAAASiOsv06j_cLFtPjh3YrUXAmjJqWqnAI2gXC5lUTLWLS",
//    // val clientSecret: String = "",
//  )

//  runBlocking {
//    val reqJson = gson.toJson(TokenReq())
//    println(reqJson)
//
//    /**
//     *   access_token: "sl.BK6JJMFNh6MHZqcI6UvGO8whJF0hd6h9rqPvXtU_eYVe8FChqPUf4-DBAJK25GkBurMYrCmfdC-inSWI9Z64qqJO0svwqFFH02dpmlV_tqO-xwQU7iQFHpi0z_VurfsHOaLlo8Vt"
//    app_root_path: "/passzero"
//    refresh_token: "gT3MoA_9K9MAAAAAAAAAASiOsv06j_cLFtPjh3YrUXAmjJqWqnAI2gXC5lUTLWLS"
//     */
//    val request = Request.Builder()
//      .url("https://api.dropboxapi.com/oauth2/token")
//      // .header("Authorization", "Bearer $expiredAccessToken")
//      // .addHeader("Content-Type", "application/json")
//      .post(
//        FormBody.Builder()
//          .addEncoded("grant_type", "refresh_token")
//          .addEncoded("client_id", "hgndvjtq97o4jls")
//          .addEncoded(
//            "refresh_token",
//            "gT3MoA_9K9MAAAAAAAAAASiOsv06j_cLFtPjh3YrUXAmjJqWqnAI2gXC5lUTLWLS"
//          )
//          .build()
//      )
//      .build()
//    println(request)
//    val response = okHttpClient.newCall(request).await()
//    println(response)
//    println(response.body!!.string())
//  }

  runBlocking {
    val client = DropboxClientImpl(
      "hgndvjtq97o4jls",
      DropboxToken(
        "sl.BK6JJMFNh6MHZqcI6UvGO8whJF0hd6h9rqPvXtU_eYVe8FChqPUf4-DBAJK25GkBurMYrCmfdC-inSWI9Z64qqJO0svwqFFH02dpmlV_tqO-xwQU7iQFHpi0z_VurfsHOaLlo8Vt",
        "gT3MoA_9K9MAAAAAAAAAASiOsv06j_cLFtPjh3YrUXAmjJqWqnAI2gXC5lUTLWLS"
      ),
      okHttpClient,
      gson,
      null
    )

    // println(client.tokenFlow.value)
    val newToken = client.refreshToken()
    println(newToken)
    println()
  }
}
