package io.github.remmerw.nott

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test

class AnnounceTest {

    @Test
    fun announceTest(): Unit = runBlocking(Dispatchers.IO) {

        val key = createRandomKey(SHA1_HASH_LENGTH)

        withTimeoutOrNull(60 * 1000) {
            val nott = newNott(nodeId(), 6001, bootstrap())
            try {
                val channel = requestAnnounce(nott, key, 3443) {
                    5000
                }

                for (address in channel) {
                    println("announce to " + address.hostname)
                }
            } finally {
                nott.shutdown()
            }
        }

        withTimeoutOrNull(30 * 1000) {

            val mdht = newNott(nodeId(), 6002, bootstrap())
            try {
                val channel = requestGetPeers(mdht, key) {
                    5000
                }

                for (address in channel) {
                    println("find from " + address.hostname)
                }
            } finally {
                mdht.shutdown()
            }

        }
    }
}