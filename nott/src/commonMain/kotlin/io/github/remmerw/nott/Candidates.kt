package io.github.remmerw.nott

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
    private val candidates: MutableMap<Peer, Node> = mutableMapOf()

    fun addCall(call: Call, peer: Peer) {
        calls[call] = peer

        checkNotNull(candidates[peer]).queried()

    }

    fun acceptResponse(call: Call): Peer? {

        if (!call.matchesExpectedID()) {
            unreachable(call)
            return null
        }

        val peer = calls[call]
        checkNotNull(peer)

        val node = candidates[peer]
        checkNotNull(node)

        return peer

    }

    fun unreachable(call: Call) {
        val peer = calls[call]
        if (peer != null) {
            val node = candidates[peer]
            node?.unreachable()
        }
    }


    fun addCandidates(entries: Set<Peer>) {
        for (peer in entries) {

            candidates.getOrPut(peer) {
                Node(peer)
            }
        }
    }


    private fun sortedLookups(): List<Node> {
        return candidates.values.sortedWith { a, b ->
            threeWayDistance(target, a.peer.id, b.peer.id)
        }
    }

    fun next(postFilter: (Peer) -> Boolean): Peer? {

        val sorted = sortedLookups()
        val node = sorted.firstOrNull { node: Node ->
            !node.isQueried() &&
                    !node.isUnreachable()
        }

        val peer = node?.peer
        if (peer != null) {
            if (postFilter.invoke(peer)) {
                return peer
            }
        }
        return null

    }

}
