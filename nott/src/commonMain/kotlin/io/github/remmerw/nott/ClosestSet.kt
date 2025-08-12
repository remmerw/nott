package io.github.remmerw.nott

import java.util.concurrent.ConcurrentHashMap

/*
* We need to detect when the closest set is stable
*  - in principle we're done as soon as there is no request candidates
*/
internal class ClosestSet(
    private val nott: Nott,
    private val target: ByteArray
) {
    private val closest: MutableSet<Peer> = mutableSetOf()
    private val calls: MutableMap<Call, Peer> = mutableMapOf()
    private val unreachable: MutableSet<Peer> = ConcurrentHashMap.newKeySet()
    private val queried: MutableSet<Peer> = ConcurrentHashMap.newKeySet()
    private val candidates: MutableSet<Peer> = ConcurrentHashMap.newKeySet()


    private fun acceptedResponse(call: Call): Peer? {

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

    private fun unreachable(call: Call) {
        val peer = calls[call]
        if (peer != null) {
            unreachable.add(peer)
        }
    }


    private fun addCandidates(entries: Set<Peer>) {
        for (peer in entries) {
            candidates.add(peer)
        }
    }

    private fun sortedLookups(): List<Peer> {
        return candidates.filter { peer ->
            !queried.contains(peer) && !unreachable.contains(peer)
        }.sortedWith { a, b ->
            threeWayDistance(target, a.id, b.id)
        }
    }

    suspend fun initialize() {
        val entries = nott.closestPeers(target, 32)
        if (entries.isEmpty()) {
            nott.bootstrap()
        } else {
            addCandidates(entries)
        }
    }

    fun nextCandidate(): Peer? {
        val sorted = sortedLookups()
        return sorted.firstOrNull { peer: Peer ->
            goodForRequest(peer)
        }
    }

    suspend fun requestCall(call: Call, peer: Peer) {
        calls[call] = peer
        queried.add(peer)
        nott.doRequestCall(call)
    }

    fun checkTimeoutOrFailure(call: Call): Boolean {
        val state = call.state()
        if (state != CallState.RESPONDED) {
            if (state == CallState.ERROR) {
                return true
            } else {
                val sendTime = call.sentTime

                if (sendTime != null) {
                    val elapsed = sendTime.elapsedNow().inWholeMilliseconds
                    if (elapsed > RESPONSE_TIMEOUT) {
                        unreachable(call)
                        nott.timeout(call)
                        call.injectError()
                        return true
                    }
                }
            }
        }
        return false
    }


    fun acceptResponse(call: Call): Peer? {
        val match = acceptedResponse(call)
        if (match != null) {
            val message = call.response
            if (message is NodesResponse) {
                val returnedNodes: MutableSet<Peer> = mutableSetOf()

                message.nodes6.filter { peer: Peer ->
                    !nott.isLocalId(peer.id)
                }.forEach { e: Peer -> returnedNodes.add(e) }

                message.nodes.filter { peer: Peer ->
                    !nott.isLocalId(peer.id)
                }.forEach { e: Peer -> returnedNodes.add(e) }

                addCandidates(returnedNodes)
            }
        }
        return match
    }

    private fun reachedTargetCapacity(): Boolean {
        return closest.size >= MAX_ENTRIES_PER_BUCKET
    }


    fun insert(peer: Peer) {
        if (closest.add(peer)) {
            if (closest.size > MAX_ENTRIES_PER_BUCKET) {
                val last = closest.sortedWith(
                    Peer.DistanceOrder(target)
                ).last()
                closest.remove(last)
            }
        }
    }

    private fun tail(): ByteArray {
        return closest.last().id
    }

    private fun head(): ByteArray {
        return closest.first().id
    }

    private fun goodForRequest(peer: Peer): Boolean {
        if (!reachedTargetCapacity()) return true

        if (threeWayDistance(target, h1 = tail(), h2 = peer.id) > 0) {
            return true
        }

        if (threeWayDistance(target, h1 = head(), h2 = peer.id) > 0) {
            return true
        }

        unreachable.add(peer)
        return false
    }

}
