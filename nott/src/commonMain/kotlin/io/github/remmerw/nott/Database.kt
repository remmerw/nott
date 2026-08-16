package io.github.remmerw.nott

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

internal class Database internal constructor() {
    private val items: MutableMap<Long, MutableSet<Address>> = ConcurrentHashMap()

    fun store(
        key: ByteArray,
        address: Address,
    ) {
        val keyEntry = items[key.toLong()]
        if (keyEntry != null) {
            keyEntry.add(address)
        } else {
            items[key.toLong()] = mutableSetOf<Address>(address)
        }
    }

    fun sample(
        key: ByteArray,
        maxEntries: Int,
    ): List<Address> {
        val keyEntry = items[key.toLong()] ?: return emptyList()

        if (keyEntry.size <= maxEntries) {
            return keyEntry.toList()
        }

        // Reservoir Sampling Algorithm
        val result = mutableListOf<Address>()
        keyEntry.forEachIndexed { index, item ->
            if (index < maxEntries) {
                result.add(item)
            } else {
                val randomIndex = Random.nextInt(index + 1)
                if (randomIndex < maxEntries) {
                    result[randomIndex] = item
                }
            }
        }
        return result
    }

    fun insertForKeyAllowed(key: ByteArray): Boolean {
        val entries = items[key.toLong()] ?: return true

        val size = entries.size

        if (size < MAX_DB_ENTRIES_PER_KEY / 5) return true

        if (size >= MAX_DB_ENTRIES_PER_KEY) return false

        return size < Random.nextInt(MAX_DB_ENTRIES_PER_KEY)
    }
}
