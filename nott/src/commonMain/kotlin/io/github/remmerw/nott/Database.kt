package io.github.remmerw.nott

import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

internal class Database internal constructor() {
    private val items: MutableMap<Long, MutableSet<InetSocketAddress>> = ConcurrentHashMap()

    fun store(
        key: ByteArray,
        address: InetSocketAddress,
    ) {
        val keyEntry = items[key.toLong()]
        if (keyEntry != null) {
            keyEntry.add(address)
        } else {
            items[key.toLong()] = mutableSetOf<InetSocketAddress>(address)
        }
    }

    fun sample(
        key: ByteArray,
        maxEntries: Int,
    ): List<InetSocketAddress> {
        val keyEntry = items[key.toLong()] ?: return emptyList()

        return keyEntry
            .asSequence()
            .shuffled()
            .take(maxEntries)
            .toList()
    }

    fun insertForKeyAllowed(key: ByteArray): Boolean {
        val entries = items[key.toLong()] ?: return true

        val size = entries.size

        if (size < MAX_DB_ENTRIES_PER_KEY / 5) return true

        if (size >= MAX_DB_ENTRIES_PER_KEY) return false

        return size < Random.nextInt(MAX_DB_ENTRIES_PER_KEY)
    }
}
