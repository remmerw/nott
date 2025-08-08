package io.github.remmerw.nott

import java.net.InetSocketAddress
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

class Peer(val id: ByteArray, val address: InetSocketAddress) {
    private var lastSeen: ValueTimeMark = TimeSource.Monotonic.markNow()
    private var failedQueries = 0


    override fun equals(other: Any?): Boolean {
        if (other is Peer) return this.equals(other)
        return false
    }

    fun equals(other: Peer?): Boolean {
        if (other == null) return false
        return id.contentEquals(other.id) && address == other.address
    }

    override fun hashCode(): Int {
        return id.hashCode() // note bucket entry
    }

    fun eligibleForNodesList(): Boolean {
        return failedQueries < 2
    }


    // old entries, e.g. from routing table reload
    private fun oldAndStale(): Boolean {
        return lastSeen.elapsedNow().inWholeMilliseconds > OLD_AND_STALE_TIME
    }

    fun needsReplacement(): Boolean {
        return (failedQueries > 2) || oldAndStale()
    }

    fun mergeInTimestamps(other: Peer) {
        if (!this.equals(other) || this === other) return
        lastSeen = newerTimeMark(lastSeen, other.lastSeen)!!
    }


    fun signalResponse() {
        lastSeen = TimeSource.Monotonic.markNow()
        failedQueries = 0
    }

    /**
     * Should be called to signal that a request to this peer has timed out;
     */
    fun signalRequestTimeout() {
        failedQueries++
    }

    override fun toString(): String {
        return "Peer(address=$address)"
    }

    class DistanceOrder(val target: ByteArray) : Comparator<Peer> {
        override fun compare(a: Peer, b: Peer): Int {
            return threeWayDistance(target, a.id, b.id)
        }
    }
}