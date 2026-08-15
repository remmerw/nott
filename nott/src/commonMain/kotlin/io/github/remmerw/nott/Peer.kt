package io.github.remmerw.nott

import java.net.InetSocketAddress
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

internal class Peer(
    val id: ByteArray,
    val address: InetSocketAddress,
) {
    private val cachedLongKey: Long by lazy { id.toLong() }

    fun key(): Long = cachedLongKey

    private var lastSeen: ValueTimeMark = TimeSource.Monotonic.markNow()
    private var failedQueries = 0

    fun eligibleForNodesList(): Boolean = failedQueries < 2

    private fun oldAndStale(): Boolean = lastSeen.elapsedNow().inWholeMilliseconds > OLD_AND_STALE_TIME

    fun needsReplacement(): Boolean = (failedQueries >= 2) || oldAndStale()

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

    override fun toString(): String = "Peer(address=$address)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Peer

        if (!id.contentEquals(other.id)) return false
        if (address != other.address) return false

        return true
    }
}
