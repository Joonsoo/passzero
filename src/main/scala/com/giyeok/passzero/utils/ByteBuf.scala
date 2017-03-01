package com.giyeok.passzero.utils

class ByteBuf(initialLength: Int) {
    private var mutable = true
    private var _pointer = 0
    private var _array = new Array[Byte](initialLength)

    def finish(): Array[Byte] = {
        mutable = false
        if (_pointer != _array.length) {
            val finalArray = new Array[Byte](_pointer)
            System.arraycopy(_array, 0, finalArray, 0, _pointer)
            _array = finalArray
        }
        _array
    }

    private def checkMutable[T](block: => T): T =
        if (mutable) block else throw new Exception("")

    private def ensureSize(size: Int): Unit = {
        if (_array.length < size) {
            var newSize = _array.length * 2
            while (newSize < size) {
                newSize *= 2
            }
            val newArray = new Array[Byte](newSize)
            System.arraycopy(_array, 0, newArray, 0, _array.length)
            _array = newArray
        }
    }

    def writeByte(value: Int): ByteBuf = checkMutable {
        ensureSize(_pointer + 1)
        _array(_pointer) = (value & 0xff).toByte
        _pointer += 1
        this
    }

    def writeBytes(bytes: Seq[Byte]): ByteBuf =
        bytes.foldLeft(this) { _.writeByte(_) }

    def writeShort(value: Int): ByteBuf =
        this.writeByte(value >> 8).writeByte(value)

    def writeInt(value: Int): ByteBuf =
        this.writeByte(value >> 24).writeByte(value >> 16).writeByte(value >> 8).writeByte(value)

    def writeLong(value: Long): ByteBuf =
        this.writeInt((value >> 32).toInt).writeInt(value.toInt)

    def writeString(value: String): ByteBuf = {
        val valueArray = value.getBytes("UTF-8")
        ensureSize(_pointer + valueArray.length)
        System.arraycopy(valueArray, 0, _array, _pointer, valueArray.length)
        _pointer += valueArray.length
        this
    }
}