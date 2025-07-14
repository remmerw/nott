package io.github.remmerw.nott

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test


class LookupTest {


    @Test
    fun fakeKey(): Unit = runBlocking(Dispatchers.IO) {

        val key = createRandomKey(SHA1_HASH_LENGTH)

        withTimeoutOrNull(60 * 1000) {

            val nott = newNott(nodeId(), 6004, bootstrap())
            try {
                val channel = requestGetPeers(nott, key) {
                    5000
                }

                for (address in channel) {
                    println("Fake " + address.hostname)
                }
            } finally {
                nott.shutdown()
            }
        }

    }
}