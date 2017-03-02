package com.giyeok.passzero.storage.googledrive

import java.io.Serializable
import java.util
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._
import com.google.api.client.util.store.DataStore
import com.google.api.client.util.store.DataStoreFactory

class LocalInfoDataStore[V <: Serializable](val id: String) extends DataStore[V] {
    private val map = new ConcurrentHashMap[String, V]()

    def getDataStoreFactory: DataStoreFactory = LocalInfoDataStoreFactory

    def set(key: String, value: V): DataStore[V] = {
        map.put(key, value)
        this
    }

    def values(): util.Collection[V] = {
        map.values()
    }

    def get(key: String): V = {
        map.get(key)
    }

    def getId: String = id

    def clear(): DataStore[V] = {
        map.clear()
        this
    }

    def size(): Int = {
        map.size()
    }

    def delete(key: String): DataStore[V] = {
        map.remove(key)
        this
    }

    def containsKey(key: String): Boolean = {
        map.containsKey(key)
    }

    def containsValue(value: V): Boolean = {
        map.containsValue(value)
    }

    def isEmpty: Boolean = {
        map.isEmpty
    }

    def keySet(): util.Set[String] = {
        map.keySet()
    }

    def printAll(): Unit = {
        println(s"================ $id")
        map.entrySet().asScala foreach { kv =>
            println(s"${kv.getKey} -> ${kv.getValue}")
        }
        println("================")
    }
}

object LocalInfoDataStoreFactory extends DataStoreFactory {
    def getDataStore[V <: Serializable](id: String): DataStore[V] =
        new LocalInfoDataStore[V](id)
}
