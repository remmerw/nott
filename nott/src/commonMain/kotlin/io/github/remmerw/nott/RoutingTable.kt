package io.github.remmerw.nott

import java.util.concurrent.ConcurrentHashMap

internal class RoutingTable internal constructor() {
    // note long key is not perfect (better would  the peer id)
    // but it is not yet really necessary (not enough peers in the routing table)
    private val entries: MutableMap<Long, Peer> = ConcurrentHashMap()

    fun insertOrRefresh(peer: Peer) {
        val entry = entries[peer.key()]
        if (entry != null) {
            refresh(peer)
        } else {
            entries[peer.key()] = peer
        }
    }

    fun refresh(peer: Peer) {
        entries[peer.key()]
            ?.mergeInTimestamps(peer)
    }

    fun closestPeers(
        key: ByteArray,
        take: Int,
    ): Set<Peer> =
        entries()
            .filter { peer -> peer.eligibleForNodesList() }
            .sortedWith { a, b ->
                threeWayDistance(key, a.id, b.id)
            }.take(take)
            .toSet()

    fun entries(): List<Peer> = entries.values.toList()

    fun onTimeout(id: ByteArray) {
        val peer = entries[id.toLong()]
        if (peer != null) {
            peer.signalFailure()
            // only removes the entry if it is bad
            if (peer.needsReplacement()) {
                entries.remove(peer.key())
            }
        }
    }

    fun findPeerById(id: ByteArray): Peer? = entries[id.toLong()]

    fun notifyOfResponse(msg: Message) {
        entries[msg.id.toLong()]?.signalResponse()
    }
}
