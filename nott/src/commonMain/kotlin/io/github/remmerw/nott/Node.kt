package io.github.remmerw.nott

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

internal class Node internal constructor(val peer: Peer) {

    @OptIn(ExperimentalAtomicApi::class)
    private val unreachable = AtomicBoolean(false)

    @OptIn(ExperimentalAtomicApi::class)
    private val queried = AtomicBoolean(false)

    @OptIn(ExperimentalAtomicApi::class)
    fun isQueried(): Boolean {
        return queried.load()
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun queried() {
        queried.store(true)
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun isUnreachable(): Boolean {
        return unreachable.load()
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun unreachable() {
        unreachable.store(true)
    }


    override fun equals(other: Any?): Boolean {
        if (other is Node) {
            return peer == other.peer
        }
        return false
    }

    override fun hashCode(): Int {
        return peer.hashCode()
    }
}
