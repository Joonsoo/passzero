package com.giyeok.passzero.storage

import com.giyeok.passzero.storage.memory.MemoryStorageProfile

trait StorageProfileSpec {
    val name: String
    val deserializer: Array[Byte] => StorageProfile
}

object Storages {
    val specs: Seq[StorageProfileSpec] = Seq(
        MemoryStorageProfile
    )

    val specNames: Seq[String] = specs map { _.name }
    assert(specNames.length == specNames.toSet.size)

    val types: Map[String, Array[Byte] => StorageProfile] =
        (specs map { spec => spec.name -> spec.deserializer }).toMap
}