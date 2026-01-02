package io.github.remmerw.nott

import java.net.InetSocketAddress
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

internal class Peer(val id: ByteArray, val address: InetSocketAddress) {
    private var lastSeen: ValueTimeMark = TimeSource.Monotonic.markNow()
    private var failedQueries = 0

    fun eligibleForNodesList(): Boolean {
        return failedQueries < 2
    }


    private fun oldAndStale(): Boolean {
        return lastSeen.elapsedNow().inWholeMilliseconds > OLD_AND_STALE_TIME
    }

    fun needsReplacement(): Boolean {
        return (failedQueries >= 2) || oldAndStale()
    }

    fun mergeInTimestamps(other: Peer) {
        lastSeen = newerTimeMark(lastSeen, other.lastSeen)!!
    }


    fun signalResponse() {
        lastSeen = TimeSource.Monotonic.markNow()
        failedQueries = 0
    }


    fun signalFailure() {
        failedQueries++
    }

    override fun toString(): String {
        return "Peer(address=$address)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Peer

        if (!id.contentEquals(other.id)) return false
        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        return id.contentHashCode()
    }


    class DistanceOrder(val target: ByteArray) : Comparator<Peer> {
        override fun compare(a: Peer, b: Peer): Int {
            return threeWayDistance(target, a.id, b.id)
        }
    }
}