package com.giyeok.passzero2.util

import java.io.DataInputStream

fun DataInputStream.readBytesFully(length: Int): ByteArray {
    val array = ByteArray(length)
    this.readFully(array, 0, length)
    return array
}

fun DataInputStream.readAll(): ByteArray {
    TODO()
}

