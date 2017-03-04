package com.giyeok.passzero.storage.googledrive

import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import scala.collection.JavaConverters._
import com.giyeok.passzero.storage.StorageProfile
import com.giyeok.passzero.storage.StorageProfileSpec
import com.giyeok.passzero.utils.ByteArrayUtil._
import com.giyeok.passzero.utils.ByteBuf
import com.giyeok.passzero.utils.ByteReader
import com.giyeok.passzero.utils.BytesInputStream
import com.giyeok.passzero.utils.BytesOutputStream
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.DataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import org.json4s.DefaultFormats
import org.json4s._
import org.json4s.native.JsonMethods._

object GoogleDriveStorageProfile extends StorageProfileSpec {
    implicit val formats = DefaultFormats

    val name: String = "googledrive"
    val deserializer: (Array[Byte] => GoogleDriveStorageProfile) = { array =>
        val reader = new ByteReader(array)

        val profileVersion = reader.readBytes(2).toSeq
        if (profileVersion != Seq(0, 1)) {
            throw new Exception("Invalid storage profile")
        }

        val applicationNameLength = reader.readInt()
        val applicationNameBytes = reader.readBytes(applicationNameLength)
        val applicationName = applicationNameBytes.asString

        val clientSecretLength = reader.readInt()
        val clientSecretBytes = reader.readBytes(clientSecretLength)
        val clientSecret = parse(clientSecretBytes.asString)

        val dataStoresSize = reader.readInt()
        val dataStores = ((0 until dataStoresSize) map { _ =>
            val idLength = reader.readInt()
            val idBytes = reader.readBytes(idLength)
            val dataStoreId = idBytes.asString

            val mapSize = reader.readInt()
            val map = ((0 until mapSize) map { _ =>
                val keyLength = reader.readInt()
                val keyBytes = reader.readBytes(keyLength)
                val key = keyBytes.asString

                val valueLength = reader.readInt()
                val valueBytes = reader.readBytes(valueLength)
                val valueIs = new ObjectInputStream(new BytesInputStream(valueBytes))
                val value: java.io.Serializable = valueIs.readObject().asInstanceOf[java.io.Serializable]

                if (valueIs.read() >= 0) {
                    throw new Exception("Invalid storage profile")
                }

                key -> value
            }).toMap

            dataStoreId -> map
        }).toMap

        if (!reader.done) {
            throw new Exception("Invalid storage profile")
        }

        new GoogleDriveStorageProfile(applicationName, clientSecret, dataStores)
    }
}

class GoogleDriveStorageProfile(applicationName: String, clientSecret: JValue, dataStores: Map[String, Map[String, java.io.Serializable]]) extends StorageProfile {
    val name: String = GoogleDriveStorageProfile.name

    def toBytes: Array[Byte] = {
        val buf = new ByteBuf(100)

        buf.writeBytes(Seq(0, 1))

        val applicationNameBytes = applicationName.toBytes
        buf.writeInt(applicationNameBytes.length)
        buf.writeBytes(applicationNameBytes)

        val clientSecretBytes = compact(render(clientSecret)).toBytes
        buf.writeInt(clientSecretBytes.length)
        buf.writeBytes(clientSecretBytes)

        buf.writeInt(dataStores.size)
        dataStores foreach { idMap =>
            val (id, map) = idMap

            val idBytes = id.toBytes
            buf.writeInt(idBytes.length)
            buf.writeBytes(idBytes)

            buf.writeInt(map.size)
            map foreach { kv =>
                val (key, value) = kv

                val keyBytes = key.toBytes
                buf.writeInt(keyBytes.length)
                buf.writeBytes(keyBytes)

                val bytesOs = new BytesOutputStream(100)
                val valueOs = new ObjectOutputStream(bytesOs)
                valueOs.writeObject(value)
                valueOs.flush()

                val valueBytes = bytesOs.finish()
                buf.writeInt(valueBytes.length)
                buf.writeBytes(valueBytes)
            }
        }

        buf.finish()
    }

    // TODO dataStoreFactory 에 의해서 dataStores에 변경사항이 생기면 LocalInfo에 반영해서 저장해야 함
    private lazy val dataStoreFactory: DataStoreFactory = ???
    private lazy val jsonFactory = JacksonFactory.getDefaultInstance
    private lazy val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private lazy val scopes = Seq(DriveScopes.DRIVE_METADATA_READONLY).asJava

    def authorize(): Credential = {
        val in = new BytesInputStream(compact(render(clientSecret)).toBytes)
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

    def getDriveService: Drive = {
        val credential = authorize()
        new Drive.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName(applicationName)
            .build()
    }

    def createSession(): GoogleDriveStorageSession = {
        new GoogleDriveStorageSession(this, getDriveService)
    }
}
