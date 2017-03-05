package com.giyeok.passzero.storage.googledrive

import java.io
import java.io.FileInputStream
import java.io.InputStreamReader
import scala.collection.JavaConverters._
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.DataStore
import com.google.api.client.util.store.DataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes

object GoogleDrivePractice {
    // 참조: https://developers.google.com/drive/v3/web/quickstart/java

    val applicationName = "passzero_app"

    private val dataStoreFactory: DataStoreFactory = {
        class DsFactory extends DataStoreFactory {
            private var map = Map[String, LocalInfoDataStore[_]]()

            private def setUpdate(): Unit = {}
            def getDataStore[V <: io.Serializable](id: String): DataStore[V] = this.synchronized {
                map get id match {
                    case Some(ds) => ds.asInstanceOf[DataStore[V]]
                    case None =>
                        val ds = new LocalInfoDataStore[V](id, Map(), this, this.setUpdate)
                        map += id -> ds
                        ds
                }
            }
        }
        new DsFactory()
    }
    // MemoryDataStoreFactory.getDefaultInstance
    // new FileDataStoreFactory(dataStoreDir)

    private val jsonFactory = JacksonFactory.getDefaultInstance

    private val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()

    private val scopes = Seq(DriveScopes.DRIVE_METADATA_READONLY).asJava

    def authorize(): Credential = {
        val in = new FileInputStream("./client_secret.json")
        val clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(in))

        val flow =
            new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, clientSecrets, scopes)
                .setDataStoreFactory(dataStoreFactory)
                .setAccessType("offline")
                .build()
        val credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user")

        val credentialDataStore = flow.getCredentialDataStore.asInstanceOf[LocalInfoDataStore[StoredCredential]]
        credentialDataStore.printAll()

        credential
    }

    def getDriveService(): Drive = {
        val credential = authorize()
        new Drive.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName(applicationName)
            .build()
    }

    def main(args: Array[String]): Unit = {
        val service = getDriveService()

        val result = service.files().list()
            .setPageSize(10)
            .setFields("nextPageToken, files(id, name)")
            .execute()
        result.getFiles.asScala foreach { file =>
            println(s"${file.getName} ${file.getId}")
        }
    }
}
