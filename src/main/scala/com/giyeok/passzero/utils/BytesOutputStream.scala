package com.giyeok.passzero.utils

import java.io.OutputStream

class BytesOutputStream(initialLength: Int) extends OutputStream {
    val buf = new ByteBuf(initialLength)

    def write(b: Int): Unit = buf.writeByte(b)

    def finish(): Array[Byte] = buf.finish()
}
