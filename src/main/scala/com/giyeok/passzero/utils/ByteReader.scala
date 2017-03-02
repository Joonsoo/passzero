package com.giyeok.passzero.utils

import com.giyeok.passzero.utils.ByteArrayUtil._

class ByteReader(array: Array[Byte], offset: Int = 0) {
    private var _pointer = offset

    def readBytes(length: Int): Array[Byte] = {
        val copied = new Array[Byte](length)
        System.arraycopy(array, _pointer, copied, 0, length)
        _pointer += length
        copied
    }

    def readRest(): Array[Byte] = {
        val copied = readBytes(array.length - _pointer)
        assert(_pointer == array.length)
        copied
    }

    def readLong(): Long = {
        val value = array.asLong(_pointer)
        _pointer += 8
        value
    }
}
