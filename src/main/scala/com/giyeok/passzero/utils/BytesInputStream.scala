package com.giyeok.passzero.utils

import java.io.InputStream

class BytesInputStream(bytes: Array[Byte]) extends InputStream {
    private val byteReader = new ByteReader(bytes)

    def read(): Int =
        if (byteReader.isFinished) -1 else byteReader.read()
}
