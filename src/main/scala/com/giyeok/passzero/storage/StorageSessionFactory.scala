package com.giyeok.passzero.storage

trait StorageSessionFactory {
    def create(storageAuth: StorageAuth): StorageSession = {
        // TODO
        new StorageSession {}
    }
}
