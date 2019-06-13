package com.giyeok.passzero2.util

import java.io.DataInputStream

fun DataInputStream.readBytesFully(length: Int): ByteArray {
    val array = ByteArray(length)
    this.readFully(array, 0, length)
    return array
}

fun DataInputStream.readAll(): ByteArray {
    return this.readBytes()
}

fun ByteArray.toHexString(): String {
    val builder = StringBuilder()

    for (i in 0 until this.size) {
        builder.append(String.format("%02x", this[i]))
    }
    return builder.toString()
}

fun String.hexStringToByteArray(): ByteArray {
    fun hexToInt(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> (c - 'a' + 10)
        in 'A'..'F' -> (c - 'A' + 10)
        else -> throw Exception("Invalid hex")
    }

    val result = ByteArray(length / 2)

    for (i in 0 until length step 2) {
        val firstIndex = hexToInt(this[i]);
        val secondIndex = hexToInt(this[i + 1]);

        val octet = firstIndex.shl(4).or(secondIndex)
        result.set(i.shr(1), octet.toByte())
    }

    return result
}
