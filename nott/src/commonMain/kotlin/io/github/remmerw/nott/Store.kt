package io.github.remmerw.nott

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.InetSocketAddress


interface Store {
    suspend fun addresses(limit: Int): List<InetSocketAddress>

    suspend fun store(address: InetSocketAddress)
}


class MemoryStore : Store {
    private val peers: MutableSet<InetSocketAddress> = mutableSetOf()
    private val mutex = Mutex()

    override suspend fun addresses(limit: Int): List<InetSocketAddress> {
        mutex.withLock {
            return peers.take(limit).toList()
        }
    }

    override suspend fun store(address: InetSocketAddress) {
        mutex.withLock {
            peers.add(address)
        }
    }
}