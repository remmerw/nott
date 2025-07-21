package io.github.remmerw.nott

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import kotlin.test.Test

class PingTest {

    @Test
    fun pingTest(): Unit = runBlocking(Dispatchers.IO) {

        val target = createRandomKey(SHA1_HASH_LENGTH) // random peer id

        val nott = newNott(nodeId(), 6005, bootstrap())
        try {
            val addresses: MutableSet<InetSocketAddress> = mutableSetOf()
            withTimeoutOrNull(30 * 1000) {
                val channel = findNode(nott, target) {
                    5000
                }
                for (address in channel) {
                    addresses.add(address)
                }
            }
            addresses.forEach { peer ->
                println(
                    "Success " + requestPing(nott, peer, target)
                            + " ping to " + target.toHexString()
                )
            }
        } finally {
            nott.shutdown()
        }
    }
}