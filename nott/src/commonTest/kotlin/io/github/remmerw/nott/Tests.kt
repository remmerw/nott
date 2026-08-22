package io.github.remmerw.nott

import io.github.remmerw.buri.BEString
import java.nio.ByteBuffer
import io.github.remmerw.buri.decodeBencode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class Tests {
    @Test
    fun testTid() {
        val tid = createTid()
        val buffer = ByteBuffer.allocate(50)
        buffer.bencodeTid(tid)
        buffer.flip()
        val data = (buffer.decodeBencode() as BEString).toByteArray()
        assertEquals(TID_LENGTH, data.size)
        val cmp = data.toLong(TID_LENGTH)
        assertEquals(tid, cmp)
    }

    @Test
    fun testLong() {
        val originalLong = 1234567890L

        val byteArray = originalLong.toByteArray(length = 4)

        val restoredLong = byteArray.toLong(length = 4)

        assertTrue(originalLong == restoredLong)
    }

    @Test
    fun testId() {
        val nodeId = nodeId()
        assertEquals(nodeId.size, 20)

        val name = nodeId.decodeToString()
        println(name)
        assertTrue(name.startsWith("-NO0815-"))
    }

    @Test
    fun testNottPort(): Unit =
        runBlocking(Dispatchers.IO) {
            val nott = newNott(nodeId())
            assertTrue(nott.port() > 0)
            nott.shutdown()
        }

    @Test
    fun defaultBootstrap(): Unit =
        runBlocking(Dispatchers.IO) {
            val nott = newNott(nodeId())

            delay(5.seconds)
            val peers = nott.closestPeers(createRandomKey(32), 32)
            assertTrue(peers.isNotEmpty())

            nott.shutdown()
        }
}
