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
        val keyEntry = items[key.toKeyLong()]
        if (keyEntry != null) {
            keyEntry.add(address)
        } else {
            items[key.toKeyLong()] = mutableSetOf<InetSocketAddress>(address)
        }
    }

    fun sample(
        key: ByteArray,
        maxEntries: Int,
    ): List<InetSocketAddress> {
        val keyEntry = items[key.contentHashCode()] ?: return emptyList()

        return keyEntry
            .asSequence()
            .shuffled()
            .take(maxEntries)
            .toList()
    }

    fun insertForKeyAllowed(key: ByteArray): Boolean {
        val entries = items[key.toKeyLong()] ?: return true

        val size = entries.size

        if (size < MAX_DB_ENTRIES_PER_KEY / 5) return true

        if (size >= MAX_DB_ENTRIES_PER_KEY) return false

        return size < Random.nextInt(MAX_DB_ENTRIES_PER_KEY)
    }
}
