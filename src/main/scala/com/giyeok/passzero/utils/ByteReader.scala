package com.giyeok.passzero.utils

import com.giyeok.passzero.utils.ByteArrayUtil._

class ByteReader(array: Array[Byte], offset: Int = 0) {
    private var _pointer = offset

    def pointer: Int = _pointer
    def done: Boolean = pointer == array.length

    def read(): Byte = {
        val b = array(_pointer)
        _pointer += 1
        b
    }

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

    def readInt(): Int = {
        val value = array.asInt(_pointer)
        _pointer += 4
        value
    }

    def readLong(): Long = {
        val value = array.asLong(_pointer)
        _pointer += 8
        value
    }
}
