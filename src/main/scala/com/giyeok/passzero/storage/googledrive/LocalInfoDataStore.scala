package com.giyeok.passzero.storage.googledrive

import java.io.Serializable
import java.util
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._
import com.google.api.client.util.store.DataStore
import com.google.api.client.util.store.DataStoreFactory

class LocalInfoDataStore[V <: Serializable](
        val id: String,
        initial: Map[String, V],
        factory: DataStoreFactory,
        updatedNotifier: () => Unit
) extends DataStore[V] {
    private val _map = new ConcurrentHashMap[String, V]()
    def map: Map[String, V] = (_map.entrySet().asScala map { entry => entry.getKey -> entry.getValue }).toMap

    private def updated(): Unit = {
        updatedNotifier()
    }

    initial foreach { kv => _map.put(kv._1, kv._2) }

    def getDataStoreFactory: DataStoreFactory = factory

    def set(key: String, value: V): DataStore[V] = {
        _map.put(key, value)
        updated()
        this
    }

    def values(): util.Collection[V] = {
        _map.values()
    }

    def get(key: String): V = {
        _map.get(key)
    }

    def getId: String = id

    def clear(): DataStore[V] = {
        _map.clear()
        updated()
        this
    }

    def size(): Int = {
        _map.size()
    }

    def delete(key: String): DataStore[V] = {
        _map.remove(key)
        updated()
        this
    }

    def containsKey(key: String): Boolean = {
        _map.containsKey(key)
    }

    def containsValue(value: V): Boolean = {
        _map.containsValue(value)
    }

    def isEmpty: Boolean = {
        _map.isEmpty
    }

    def keySet(): util.Set[String] = {
        _map.keySet()
    }

    def printAll(): Unit = {
        println(s"================ $id")
        _map.entrySet().asScala foreach { kv =>
            println(s"${kv.getKey} -> ${kv.getValue}")
        }
        println("================")
    }
}
