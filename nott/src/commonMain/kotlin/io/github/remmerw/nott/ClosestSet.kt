package io.github.remmerw.nott

/*
* We need to detect when the closest set is stable
*  - in principle we're done as soon as there is no request candidates
*/
internal class ClosestSet(
    private val nott: Nott,
    private val target: ByteArray,
) {
    private val closest: MutableSet<Peer> = mutableSetOf()
    private val queried: MutableSet<Long> = sortedSetOf()
    private val candidates: MutableSet<Peer> = mutableSetOf()

    private fun acceptedResponse(call: Call): Peer? {
        if (!call.matchesExpectedID()) {
            return null
        }
        val rsp = call.response
        if (rsp != null) {
            return nott.findPeerById(rsp.id)
        }
        return null
    }

    private fun addCandidates(entries: Set<Peer>) {
        for (peer in entries) {
            if (goodForRequest(peer)) {
                val key = peer.key()
                if (!queried.contains(key)) {
                    candidates.add(peer)
                }
            }
        }
    }

    private fun sortedLookups(): List<Peer> =
        candidates
            .filter { peer ->
                val key = peer.key()
                !queried.contains(key)
            }.sortedWith { a, b ->
                threeWayDistance(target, a.id, b.id)
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
        return sorted.firstOrNull()
    }

    suspend fun requestCall(
        request: Request,
        peer: Peer,
    ): Call {
        queried.add(peer.key())
        candidates.remove(peer)
        return nott.doRequestCall(request, peer.id)
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

                message.nodes6
                    .filter { peer: Peer ->
                        !nott.isLocalId(peer.id)
                    }.forEach { e: Peer -> returnedNodes.add(e) }

                message.nodes
                    .filter { peer: Peer ->
                        !nott.isLocalId(peer.id)
                    }.forEach { e: Peer -> returnedNodes.add(e) }

                addCandidates(returnedNodes)
            }
        }
        return match
    }

    private fun reachedTargetCapacity(): Boolean = closest.size >= MAX_ENTRIES_PER_BUCKET

    fun insert(peer: Peer) {
        if (closest.add(peer)) {
            if (closest.size > MAX_ENTRIES_PER_BUCKET) {
                val last =
                    closest
                        .sortedWith { a, b ->
                            threeWayDistance(target, a.id, b.id)
                        }.last()
                closest.remove(last)
            }
        }
    }

    private fun tail(): ByteArray = closest.last().id

    private fun head(): ByteArray = closest.first().id

    private fun goodForRequest(peer: Peer): Boolean {
        if (!reachedTargetCapacity()) return true

        if (threeWayDistance(target, h1 = tail(), h2 = peer.id) > 0) {
            return true
        }

        if (threeWayDistance(target, h1 = head(), h2 = peer.id) > 0) {
            return true
        }

        return false
    }
}
