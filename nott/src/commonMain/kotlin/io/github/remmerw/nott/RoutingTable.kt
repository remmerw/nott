package io.github.remmerw.nott

import java.util.concurrent.ConcurrentHashMap

internal class RoutingTable internal constructor() {
    private val entries: MutableMap<Long, Peer> = ConcurrentHashMap()

    fun insert(peer: Peer) {
        entries[peer.key()] = peer
    }

    fun closestPeers(
        key: ByteArray,
        take: Int,
    ): List<Peer> =
        entries.values
            .sortedWith { a, b ->
                threeWayDistance(key, a.id, b.id)
            }.take(take)

    fun remove(id: ByteArray) {
        entries.remove(id.toLong())
    }

    fun findPeerById(id: ByteArray): Peer? = entries[id.toLong()]
}
