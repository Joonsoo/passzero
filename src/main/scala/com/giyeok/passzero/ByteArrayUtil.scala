package com.giyeok.passzero

import java.util.Base64

object ByteArrayUtil {

    implicit class Implicit(array: Array[Byte]) {
        def toHexString: String =
            (array map { x => f"$x%02x" }).mkString

        def toBase64: String =
            Base64.getEncoder.encodeToString(array)

        def xor(other: Array[Byte]): Array[Byte] = {
            val newArray = new Array[Byte](Math.max(array.length, other.length))
            for (i <- newArray.indices) {
                val a = if (i < array.length) array(i) else 0
                val b = if (i < other.length) other(i) else 0
                newArray(i) = (a ^ b).toByte
            }
            newArray
        }
    }
}