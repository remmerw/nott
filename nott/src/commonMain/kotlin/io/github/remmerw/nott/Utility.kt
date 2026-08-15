package io.github.remmerw.nott

import java.util.Arrays
import kotlin.math.min

internal fun ByteArray.toLongKey(): Long {
    return this.toLong()
}

internal fun ByteArray.toLong(length: Int = 8): Long {
    require(length in 1..8) { "Length must be between 1 and 8 bytes for a Long value." }
    require(this.size >= length) { "Array is too small for the requested length." }

    var result = 0L
    for (i in 0 until length) {
        val byteValue = this[i].toLong() and 0xFFL
        result = (result shl 8) or byteValue
    }
    return result
}

internal fun Long.toByteArray(length: Int = 8): ByteArray {
    require(length in 1..8) { "Length must be between 1 and 8 bytes for a Long value." }

    val result = ByteArray(length)
    for (i in 0 until length) {
        val shiftBits = (length - 1 - i) * 8
        result[i] = ((this shr shiftBits) and 0xFFL).toByte()
    }
    return result
}


@Suppress("unused")
internal fun mismatch(
    a: ByteArray,
    b: ByteArray,
): Int {
    val min = min(a.size, b.size)
    for (i in 0 until min) {
        if (a[i] != b[i]) return i
    }

    return if (a.size == b.size) -1 else min
}

/**
 * Compares the distance of two keys relative to this one using the XOR metric
 *
 * @return -1 if h1 is closer to this key, 0 if h1 and h2 are equidistant, 1 if h2 is closer
 */
internal fun threeWayDistance(
    h0: ByteArray,
    h1: ByteArray,
    h2: ByteArray,
): Int {
    val mmi = Arrays.mismatch(h1, h2)
    if (mmi == -1) return 0

    val h = h0[mmi].toUByte()
    val a = h1[mmi].toUByte()
    val b = h2[mmi].toUByte()

    return (a xor h).compareTo(b xor h)
}
