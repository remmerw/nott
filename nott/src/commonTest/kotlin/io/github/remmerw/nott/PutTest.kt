package io.github.remmerw.nott


import io.github.remmerw.borr.Ed25519Sign
import io.github.remmerw.buri.BEString
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class PutTest {

    @OptIn(ExperimentalTime::class, ExperimentalAtomicApi::class)
    @Test
    fun putTest(): Unit = runBlocking(Dispatchers.IO) {


        // https://www.bittorrent.org/beps/bep_0044.html

        val data = "moin".encodeToByteArray()

        val keys = Ed25519Sign.KeyPair.newKeyPair()


        val v = BEString(data)
        val cas: Long? = null
        val k: ByteArray = keys.getPublicKey()
        val salt: ByteArray? = null
        val seq: Long = Clock.System.now().toEpochMilliseconds()
        val signBuffer = Buffer()
        signBuffer.write("3:seqi".encodeToByteArray())
        signBuffer.write(seq.toString().encodeToByteArray())
        signBuffer.write("e1:v".encodeToByteArray())
        signBuffer.write(data.size.toString().encodeToByteArray())
        signBuffer.write(":".encodeToByteArray())
        signBuffer.write(data)

        val signer = Ed25519Sign(keys.getPrivateKey())
        val sig = signer.sign(signBuffer.readByteArray())

        val target = sha1(k)

        val peers = mutableListOf<Peer>()
        val added = AtomicInt(0)
        withTimeout(60 * 1000) {
            val nott = newNott(nodeId())
            try {
                val channel = requestPut(
                    nott, target, v, cas, k, salt, seq, sig
                ) {
                    5000
                }

                for (peer in channel) {
                    println("put to $peer")
                    peers.add(peer)
                    if (added.incrementAndFetch() > 20) {
                        coroutineContext.cancelChildren()
                    }
                }
            } catch (_: CancellationException) {

            } finally {
                nott.shutdown()
            }
        }

        delay(5000)

        val read = AtomicInt(0)
        withTimeoutOrNull(30 * 1000) {
            val nott = newNott(nodeId(), peers = peers)
            try {
                val channel = requestGet(nott, target) {
                    5000
                }

                for (data in channel) {
                    println(
                        "data received " + data.data.toString() + " " +
                                data.k?.decodeToString()
                    )
                    if (read.incrementAndFetch() > 5) {
                        coroutineContext.cancelChildren()
                    }
                }
            } catch (_: CancellationException) {
            } finally {
                nott.shutdown()
            }
        }

        assertTrue(read.load() >= 5)
    }
}