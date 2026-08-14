package io.github.remmerw.nott

import java.util.concurrent.ConcurrentHashMap

internal class RoutingTable internal constructor() {
    // note long key is not perfect (better would  the peer id)
    // but it is not yet really necessary (not enough peers in the routing table)
    private val entries: MutableMap<Long, Peer> = ConcurrentHashMap()

    fun insertOrRefresh(peer: Peer) {
        val entry = entries[peer.id.toLongKey()]
        if (entry != null) {
            refresh(peer)
        } else {
            entries[peer.id.toLongKey()] = peer
        }
    }

    fun refresh(peer: Peer) {
        entries[peer.id.toLongKey()]
            ?.mergeInTimestamps(peer)
    }

    fun closestPeers(
        key: ByteArray,
        take: Int,
    ): Set<Peer> =
        entries()
            .filter { peer -> peer.eligibleForNodesList() }
            .sortedWith(Peer.DistanceOrder(key))
            .take(take)
            .toSet()

    fun entries(): List<Peer> = entries.values.toList()

    fun onTimeout(id: ByteArray) {
        val peer = entries[id.toLongKey()]
        if (peer != null) {
            peer.signalFailure()
            // only removes the entry if it is bad
            if (peer.needsReplacement()) {
                entries.remove(peer.id.toLongKey()())
            }
        }
    }

    fun findPeerById(id: ByteArray): Peer? = entries[id.toLongKey()]

    fun notifyOfResponse(msg: Message) {
        entries[msg.id.toLongKey()]?.signalResponse()
    }
}
