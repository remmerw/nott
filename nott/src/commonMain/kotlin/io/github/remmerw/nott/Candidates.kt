package io.github.remmerw.nott

import java.net.InetSocketAddress

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
    private val callsByIp: MutableMap<InetSocketAddress, MutableSet<Call>> = mutableMapOf()
    private val candidates: MutableMap<Peer, Node> = mutableMapOf()


    private fun lookup(node: Node): Boolean {

        if (node.isUnreachable()) return false

        val peer = node.peer
        val addr = peer.address


        // only do requests to nodes which have at least one source where the source
        // has not given us lots of bogus candidates
        if (node.nonSuccessfulDescendants()) return false


        // also check other calls based on matching IP instead of strictly matching ip+port+id
        val byIp: Set<Call>? = callsByIp[addr]
        if (byIp != null) {

            for (c in byIp) {
                // in flight, not stalled
                if (c.state() == CallState.SENT || c.state() == CallState.UNSENT) return false

                // already got a response from that addr that does not match what we would expect from this candidate anyway
                if (c.state() == CallState.RESPONDED && !c.response!!.id.contentEquals(
                        peer.id
                    )
                ) return false
                // we don't strictly check the presence of IDs in error messages, so we can't compare those here
                if (c.state() == CallState.ERROR) return false
            }

        }

        return true
    }


    fun addCall(call: Call, peer: Peer) {
        calls[call] = peer

        val byIp = callsByIp.getOrPut(call.request.address) { mutableSetOf() }
        byIp.add(call)

        checkNotNull(candidates[peer]).addCall(call)

    }

    fun acceptResponse(call: Call): Peer? {

        if (!call.matchesExpectedID()) return null

        val peer = calls[call]
        checkNotNull(peer)

        val node = candidates[peer]
        checkNotNull(node)


        node.accept()
        return peer

    }

    fun unreachable(call: Call) {
        val peer = calls[call]
        if(peer != null) {
            val node = candidates[peer]
            node?.unreachable()
        }
    }


    fun addCandidates(source: Peer?, entries: Set<Peer>) {

        val sourceNode = if (source != null) candidates[source] else null

        val children: MutableList<Node> = mutableListOf()

        for (peer in entries) {

            val node = candidates.getOrPut(peer) {
                Node(peer)
            }

            if (sourceNode != null) node.addSource(sourceNode)
            children.add(node)
        }

        sourceNode?.addChildren(children)

    }


    private fun sortedLookups(): List<Node> {
        return candidates.values.sortedWith { a, b ->
            val res = threeWayDistance(target, a.peer.id, b.peer.id)
            if (res == 0) {
                b.numSources() - a.numSources()
            } else {
                res
            }
        }
    }

    fun next(postFilter: (Peer) -> Boolean): Peer? {

        // sort + filter + findAny should be faster than filter + min in
        // this case since findAny reduces the invocations of the filter,
        // and that is more expensive than the sorting

        val sorted = sortedLookups()
        var node = sorted
            .filter { node: Node -> node.hasNoCalls() }.firstOrNull { node ->
                lookup(node)
            }

        var peer = node?.peer
        if (peer != null) {
            if (postFilter.invoke(peer)) {
                return peer
            }
        }

        node = sorted.firstOrNull { node -> lookup(node) }
        peer = node?.peer
        if (peer != null) {
            if (postFilter.invoke(peer)) {
                return peer
            }
        }

        return null

    }

    fun nodeForEntry(e: Peer): Node? {
        return candidates[e]
    }


}
