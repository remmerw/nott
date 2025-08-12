package io.github.remmerw.nott

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test


class LookupTest {


    @Test
    fun lookupTest(): Unit = runBlocking(Dispatchers.IO) {

        val key = createRandomKey(SHA1_HASH_LENGTH) // Note: it is a fake key

        withTimeoutOrNull(30 * 1000) {

            val nott = newNott(nodeId())
            try {
                val channel = requestGetPeers(nott, key) {
                    5000
                }

                for (response in channel) {
                    println("Fake from ${response.peer} " + response.addresses)
                }
            } finally {
                nott.shutdown()
            }
        }

    }
}