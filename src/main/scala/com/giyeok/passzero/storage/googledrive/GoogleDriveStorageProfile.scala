package com.giyeok.passzero.storage.googledrive

import com.giyeok.passzero.storage.StorageProfile
import com.giyeok.passzero.storage.StorageProfileSpec
import org.json4s.DefaultFormats

object GoogleDriveStorageProfile extends StorageProfileSpec {
    implicit val formats = DefaultFormats

    val name: String = "googledrive"
    val deserializer: (Array[Byte] => GoogleDriveStorageProfile) = { array =>
        ???
    }
}

class GoogleDriveStorageProfile extends StorageProfile {
    val name: String = GoogleDriveStorageProfile.name

    def toBytes: Array[Byte] = ???

    def createSession(): GoogleDriveStorageSession =
        new GoogleDriveStorageSession
}
