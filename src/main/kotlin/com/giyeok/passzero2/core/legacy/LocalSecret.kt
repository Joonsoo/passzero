package com.giyeok.passzero2.core.legacy

import com.giyeok.passzero2.core.LocalInfoProto
import com.google.protobuf.ByteString

object LegacyLocalSecret {
  fun fromBytes(bytes: ByteString): LocalInfoProto.LocalSecret {
    check(bytes.size() == 64)
    return LocalInfoProto.LocalSecret.newBuilder()
      .setPasswordSalt(bytes.substring(0, 32))
      .setLocalKey(bytes.substring(32))
      .build()
  }
}
