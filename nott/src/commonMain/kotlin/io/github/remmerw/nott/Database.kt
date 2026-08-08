package io.github.remmerw.nott

import java.util.concurrent.ConcurrentHashMap
import java.net.InetSocketAddress
import kotlin.random.Random

internal class Database internal constructor() {
    private val items: MutableMap<Int, MutableSet<InetSocketAddress>> = ConcurrentHashMap()

    fun store(key: ByteArray, address: InetSocketAddress) {

        val keyEntry = items[key.contentHashCode()]
        if (keyEntry != null) {
            keyEntry.add(address)
        } else {
            items[key.contentHashCode()] = mutableSetOf<InetSocketAddress>(address)
        }

    }

    fun sample(key: ByteArray, maxEntries: Int): List<InetSocketAddress> {

        val keyEntry = items[key.contentHashCode()] ?: return emptyList()

return keyEntry.asSequence().shuffled().take(maxEntries).toList()

    }


    fun insertForKeyAllowed(key: ByteArray): Boolean {

        val entries = items[key.contentHashCode()] ?: return true

        val size = entries.size

        if (size < MAX_DB_ENTRIES_PER_KEY / 5) return true

        if (size >= MAX_DB_ENTRIES_PER_KEY) return false

        return size < Random.nextInt(MAX_DB_ENTRIES_PER_KEY)

    }

}
