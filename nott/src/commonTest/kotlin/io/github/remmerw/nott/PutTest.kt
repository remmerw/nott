package io.github.remmerw.nott


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.time.ExperimentalTime

class PutTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun putTest(): Unit = runBlocking(Dispatchers.IO) {

        /** TODO activate again when Borr supports IOS

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


        withTimeoutOrNull(60 * 1000) {
            val nott = newNott(nodeId(), 6006, bootstrap())
            try {
                val channel = requestPut(
                    nott, target, v, cas, k, salt, seq, sig
                ) {
                    5000
                }

                for (address in channel) {
                    println("put to " + address.hostname)
                }
            } finally {
                nott.shutdown()
            }
        }


        withTimeoutOrNull(30 * 1000) {
            val nott = newNott(nodeId(), 6007, bootstrap())
            try {
                val channel = requestGet(nott, target) {
                    5000
                }

                for (data in channel) {
                    println("data received " + data.data.toString() + " " + data.k?.encodeBase64())
                }
            } finally {
                nott.shutdown()
            }
        }
         */
    }

}