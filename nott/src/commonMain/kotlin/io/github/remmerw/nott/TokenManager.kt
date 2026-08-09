package io.github.remmerw.nott

import org.kotlincrypto.hash.sha1.SHA1
import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.time.TimeSource

internal class TokenManager {
    private var currentStamp: Long = 0L
    private var previousStamp: Long = 0L
    private val sessionSecret = createRandomKey(SHA1_HASH_LENGTH)
    private var timeSource = TimeSource.Monotonic.markNow()

    private fun update() {
        if (timeSource.elapsedNow().inWholeMilliseconds > TOKEN_TIMEOUT) {
            timeSource = TimeSource.Monotonic.markNow()
            previousStamp = currentStamp
            currentStamp = Random.nextLong()
        }
    }

    fun checkToken(
        token: ByteArray,
        nodeId: ByteArray,
        address: ByteArray,
        port: UShort,
        lookup: ByteArray,
    ): Boolean {
        update()
        return checkToken(token, nodeId, address, port, lookup, currentStamp) ||
            checkToken(token, nodeId, address, port, lookup, previousStamp)
    }

    fun generateToken(
        nodeId: ByteArray,
        address: ByteArray,
        port: UShort,
        key: ByteArray,
    ): ByteArray {
        update()

        // generate a hash of the ip port and the current time
        // should prevent anybody from crapping things up
        val digest = SHA1()
        digest.update(nodeId)
        digest.update(address)
        digest.update(
            ByteBuffer
                .allocate(2)
                .putShort(port.toShort())
                .array(),
        )
        digest.update(ByteBuffer.allocate(8).putLong(currentStamp).array())
        digest.update(key)
        digest.update(sessionSecret)

        // shorten 4bytes to not waste packet size
        // the chance of guessing correctly would be 1 : 4 million
        // and only be valid for a single infohash

        return digest.digest().copyOf(4)
    }

    private fun checkToken(
        token: ByteArray,
        nodeId: ByteArray,
        address: ByteArray,
        port: UShort,
        lookup: ByteArray,
        timeStamp: Long,
    ): Boolean {
        val digest = SHA1()
        digest.update(nodeId)
        digest.update(address)
        digest.update(
            ByteBuffer
                .allocate(2)
                .putShort(port.toShort())
                .array(),
        )
        digest.update(ByteBuffer.allocate(8).putLong(timeStamp).array())
        digest.update(lookup)
        digest.update(sessionSecret)
        val rawToken = digest.digest().copyOf(4)

        return token.contentEquals(rawToken)
    }
}
