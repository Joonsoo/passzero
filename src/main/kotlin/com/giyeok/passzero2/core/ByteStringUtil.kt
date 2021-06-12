package com.giyeok.passzero2.core

import com.google.protobuf.ByteString
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

fun ByteString.toInputStream(): ByteArrayInputStream =
  ByteArrayInputStream(this.toByteArray())

fun ByteArrayInputStream.readShort(): Short {
  val b0 = read()
  val b1 = read()
  return (((b0 and 0xff) shl 8) or (b1 and 0xff)).toShort()
}

fun ByteArrayInputStream.readInt(): Int {
  val b0 = read()
  val b1 = read()
  val b2 = read()
  val b3 = read()
  return ((b0 and 0xff) shl 24) or ((b1 and 0xff) shl 16) or ((b2 and 0xff) shl 8) or (b3 and 0xff)
}

fun ByteArrayInputStream.readLong(): Long {
  val b0 = read().toLong()
  val b1 = read().toLong()
  val b2 = read().toLong()
  val b3 = read().toLong()
  val b4 = read().toLong()
  val b5 = read().toLong()
  val b6 = read().toLong()
  val b7 = read().toLong()
  return ((b0 and 0xff) shl 56) or ((b1 and 0xff) shl 48) or ((b2 and 0xff) shl 40) or ((b3 and 0xff) shl 32) or
    ((b4 and 0xff) shl 24) or ((b5 and 0xff) shl 16) or ((b6 and 0xff) shl 8) or (b7 and 0xff)
}

fun ByteArrayInputStream.readBytes(length: Int): ByteString = ByteString.copyFrom(this.readNBytes(length))

fun ByteArrayInputStream.readRest(): ByteString = ByteString.readFrom(this)

fun ByteArrayOutputStream.writeLong(value: Long) {
  writeInt(((value shr 32) and 0xffffffff).toInt())
  writeInt((value and 0xffffffff).toInt())
}

fun ByteArrayOutputStream.writeInt(value: Int) {
  write((value shr 24) and 0xff)
  write((value shr 16) and 0xff)
  write((value shr 8) and 0xff)
  write((value) and 0xff)
}

fun ByteArrayOutputStream.writeBytes(bytes: ByteString) {
  bytes.writeTo(this)
}

fun ByteString.xor(other: ByteString): ByteString {
  check(this.size() == other.size())
  return ByteString.copyFrom(this.zip(other).map { (a, b) -> (a.toInt() xor b.toInt()).toByte() }.toByteArray())
}
