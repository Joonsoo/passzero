package com.giyeok.passzero2.core.storage

import com.giyeok.passzero2.core.Encryption
import com.giyeok.passzero2.core.LocalInfoProto
import com.giyeok.passzero2.core.LocalInfoProto.StorageProfile
import com.giyeok.passzero2.core.LocalInfoWithRevision
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class EncryptionTest {
  @Test
  fun test() {
    val localSecret = Encryption.generateLocalSecret()
    val localInfo = LocalInfoProto.LocalInfo.newBuilder()
      .setSecret(localSecret)
      .setStorageProfile(
        StorageProfile.newBuilder()
          .setDropbox(
            LocalInfoProto.DropboxStorageProfile.newBuilder()
              .setAccessToken("access_token")
              .setAppName("passzero")
              .setAppRootPath("/path/to/root")
          )
      ).build()
    val liwr = LocalInfoWithRevision(100, localInfo)
    val password = "arbitrary password"
    val encoded = liwr.encode(password)
    val decoded = LocalInfoWithRevision.decode(password, encoded)

    assertThat(decoded.localInfoWithRevision).isEqualTo(liwr)
  }
}
