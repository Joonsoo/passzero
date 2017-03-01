package com.giyeok.passzero.utils

import java.util.Base64

object ByteArrayUtil {
    val charsetName = "UTF-8"

    def hexLine(hex: Seq[Byte]): String =
        hex map { x => f"$x%02x" } grouped 4 map { _ mkString " " } mkString "  "

    val hexMatrixTitle: String =
        hexLine((0 until 16) map { _.toByte })

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

        def toHexMatrix: Seq[String] = {
            (array.grouped(16) map { line =>
                hexLine(line)
            }).toSeq
        }

        def printHexMatrix(title: Boolean = true): Unit = {
            if (title) {
                val lineIndexLength = 4
                println("=" * ((lineIndexLength + 1) + hexMatrixTitle.length))
                println(" " * (lineIndexLength + 1) + hexMatrixTitle)
                println("-" * ((lineIndexLength + 1) + hexMatrixTitle.length))
                Stream.from(0, 16) zip this.toHexMatrix foreach { pair =>
                    val (startPoint, line) = pair
                    val startPointHex = startPoint.toHexString
                    val padding = "0" * (lineIndexLength - startPointHex.length)
                    println(f"$padding$startPointHex $line")
                }
                println("=" * ((lineIndexLength + 1) + hexMatrixTitle.length))
            } else {
                this.toHexMatrix foreach println
            }
        }

        def toBase64: String =
            Base64.getEncoder.encodeToString(array)

        def asLong(offset: Int): Long = {
            (array(offset).toLong << 56) | (array(offset + 1).toLong << 48) |
                (array(offset + 2).toLong << 40) | (array(offset + 3).toLong << 32) |
                (array(offset + 4).toLong << 24) | (array(offset + 5).toLong << 16) |
                (array(offset + 6).toLong << 8) | array(offset + 7).toLong
        }

        def asLong: Long = asLong(0) ensuring array.length == 8

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