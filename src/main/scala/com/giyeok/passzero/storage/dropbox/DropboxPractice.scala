package com.giyeok.passzero.storage.dropbox

import scala.collection.JavaConverters._
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.giyeok.passzero.utils.ByteArrayUtil._
import com.giyeok.passzero.utils.BytesInputStream
import com.giyeok.passzero.utils.BytesOutputStream

object DropboxPractice {

    def main(args: Array[String]): Unit = {
        val accessToken = ???
        val config = DbxRequestConfig.newBuilder("passzero").build()
        val client = new DbxClientV2(config, accessToken)

        val result = client.files().listFolder("")
        result.getEntries.asScala foreach { meta =>
            println(meta.getName, meta.getPathDisplay, meta.getPathLower, meta.getParentSharedFolderId, meta.isInstanceOf[FolderMetadata], meta.isInstanceOf[FileMetadata])
        }

        // client.files().createFolder("/passzero/142332/123123")
        client.files().uploadBuilder("/passzero/142332/123123/abc").uploadAndFinish(new BytesInputStream("Hello!".toBytes))
        val os = new BytesOutputStream(100)
        client.files().download("/passzero/142332/123123/abc").download(os)
        println(os.finish().asString)

        println(client.files().getMetadata("/passzero"))
        println(client.files().getMetadata("/passzeo"))
        println("===")
    }
}
