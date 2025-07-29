package io.github.remmerw.nott

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test


class LookupTest {


    @Test
    fun lookupTest(): Unit = runBlocking(Dispatchers.IO) {

        val key = createRandomKey(SHA1_HASH_LENGTH) // Note: it is a fake key

        withTimeoutOrNull(60 * 1000) {

            val nott = newNott(nodeId())
            try {
                val channel = requestGetPeers(nott, key) {
                    5000
                }

                for (address in channel) {
                    println("Fake $address")
                }
            } finally {
                nott.shutdown()
            }
        }

    }
}