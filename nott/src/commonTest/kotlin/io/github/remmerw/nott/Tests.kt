package io.github.remmerw.nott

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Tests {
    @Test
    fun testId() {
        val nodeId = nodeId()
        assertEquals(nodeId.size, 20)

        val name = nodeId.decodeToString()
        println(name)
        assertTrue(name.startsWith("-NO0815-"))
    }

    @Test
    fun testNottPort(): Unit = runBlocking(Dispatchers.IO) {
        val nott = newNott(nodeId())
        assertTrue(nott.port() > 0)
        nott.shutdown()
    }



    @Test
    fun defaultBootstrap(): Unit = runBlocking(Dispatchers.IO) {
        val nott = newNott(nodeId())

        delay(5000)
        val peers = nott.closestPeers(createRandomKey(32), 32)
        assertTrue(peers.isNotEmpty())

        nott.shutdown()
    }
}