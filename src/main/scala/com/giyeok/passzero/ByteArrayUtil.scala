package com.giyeok.passzero

import java.util.Base64

object ByteArrayUtil {
    val charsetName = "UTF-8"

    def fill(length: Int, value: Int): Array[Byte] = {
        assert((value.toByte.toInt & 0xff) == value)
        val a = new Array[Byte](length)
        a.indices foreach { a(_) = value.toByte }
        a
    }

    implicit class ImplicitString(string: String) {
        def toBytes: Array[Byte] = string.getBytes(charsetName)
    }

    implicit class ImplicitLong(long: Long) {
        def toBytes: Array[Byte] = ???
    }

    implicit class Implicit(array: Array[Byte]) {
        def toHexString: String =
            (array map { x => f"$x%02x" }).mkString

        def toBase64: String =
            Base64.getEncoder.encodeToString(array)

        def asLong: Long = {
            ???
        }

        def asString: String =
            new String(array, charsetName)

        def xor(other: Array[Byte]): Array[Byte] = {
            val newArray = new Array[Byte](Math.max(array.length, other.length))
            for (i <- newArray.indices) {
                val a = if (i < array.length) array(i) else 0
                val b = if (i < other.length) other(i) else 0
                newArray(i) = (a ^ b).toByte
            }
            newArray
        }

        def identical(other: Array[Byte]): Boolean =
            if (array.length != other.length) false else {
                (array zip other) forall { x => x._1 == x._2 }
            }
    }
}