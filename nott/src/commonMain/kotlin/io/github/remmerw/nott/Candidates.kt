package io.github.remmerw.nott

import java.util.concurrent.ConcurrentHashMap

/*
* Issues:
*
* - spurious packet loss
* - remotes might fake IDs. possibly with collusion.
* - invalid results
*   - duplicate IPs
*   - duplicate IDs
*   - wrong IDs -> might trip up fake ID detection!
*   - IPs not belonging to DHT nodes -> DoS abuse
*
* Solution:
*
* - generally avoid querying an IP more than once
* - dedup result lists from each node
* - ignore responses with unexpected IDs. normally this could be abused to silence others, but...
* - allow duplicate requests if many *separate* sources suggest precisely the same <id, ip, port> tuple
*
* -> we can recover from all the above-listed issues because the terminal set of nodes should
* have some partial agreement about their neighbors
*
*/
internal class Candidates internal constructor(
    private val target: ByteArray
) {
    private val calls: MutableMap<Call, Peer> = mutableMapOf()
    private val unreachable: MutableSet<Peer> = ConcurrentHashMap.newKeySet()
    private val queried: MutableSet<Peer> = ConcurrentHashMap.newKeySet()
    private val candidates: MutableSet<Peer> = ConcurrentHashMap.newKeySet()

    fun addCall(call: Call, peer: Peer) {
        calls[call] = peer

        queried.add(peer)

    }

    fun acceptResponse(call: Call): Peer? {

        if (!call.matchesExpectedID()) {
            unreachable(call)
            return null
        }

        val peer = calls[call]
        checkNotNull(peer)

        if (candidates.contains(peer)) {
            return peer
        }

        return null

    }

    fun unreachable(call: Call) {
        val peer = calls[call]
        if (peer != null) {
            unreachable.add(peer)
        }
    }


    fun addCandidates(entries: Set<Peer>) {
        for (peer in entries) {
            candidates.add(peer)
        }
    }


    private fun sortedLookups(): List<Peer> {
        return candidates.sortedWith { a, b ->
            threeWayDistance(target, a.id, b.id)
        }
    }

    fun next(postFilter: (Peer) -> Boolean): Peer? {

        val sorted = sortedLookups()
        val peer = sorted.firstOrNull { peer: Peer ->
            !queried.contains(peer) && !unreachable.contains(peer)
        }

        if (peer != null) {
            if (postFilter.invoke(peer)) {
                return peer
            }
        }
        return null

    }

}
