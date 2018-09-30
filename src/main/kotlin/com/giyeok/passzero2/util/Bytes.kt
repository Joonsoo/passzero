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
