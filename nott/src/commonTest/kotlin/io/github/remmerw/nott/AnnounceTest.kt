package io.github.remmerw.nott

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class AnnounceTest {

    @Test
    fun announceTest(): Unit = runBlocking(Dispatchers.IO) {

        val key = createRandomKey(SHA1_HASH_LENGTH)

        withTimeoutOrNull(20.seconds) {
            val nott = newNott(nodeId())
            try {
                val channel = requestAnnounce(nott, key, 3443) {
                    5000
                }

                for (address in channel) {
                    println("announce to $address")
                }
            } finally {
                nott.shutdown()
            }
        }

        withTimeoutOrNull(10.seconds) {

            val nott = newNott(nodeId())
            try {
                val channel = requestGetPeers(nott, key) {
                    5000
                }

                for (response in channel) {
                    println("find from ${response.peer} " + response.addresses)
                }
            } finally {
                nott.shutdown()
            }

        }
    }
}