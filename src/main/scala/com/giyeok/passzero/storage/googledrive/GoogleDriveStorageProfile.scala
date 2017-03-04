package com.giyeok.passzero.storage.googledrive

import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import com.giyeok.passzero.storage.StorageProfile
import com.giyeok.passzero.storage.StorageProfileSpec
import com.giyeok.passzero.utils.ByteArrayUtil._
import com.giyeok.passzero.utils.ByteBuf
import com.giyeok.passzero.utils.ByteReader
import com.giyeok.passzero.utils.BytesInputStream
import com.giyeok.passzero.utils.BytesOutputStream
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

    def createSession(): GoogleDriveStorageSession =
        new GoogleDriveStorageSession
}
