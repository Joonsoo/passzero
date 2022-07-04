package com.giyeok.passzero2.core

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend inline fun Call.await(): Response {
  return suspendCancellableCoroutine { continuation ->
    this.enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        continuation.resumeWithException(e)
      }

      override fun onResponse(call: Call, response: Response) {
        if (response.code == 200) {
          continuation.resume(response)
        } else {
          val bodyString = response.body?.string()
          response.body?.close()
          continuation.resumeWithException(UnsuccessfulResponseException(response, bodyString))
        }
      }
    })
  }
}

data class UnsuccessfulResponseException(val response: Response, val responseBody: String?) :
  Exception("Unsuccessful response: $response, body=$responseBody")
