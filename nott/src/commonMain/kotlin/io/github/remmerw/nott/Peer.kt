package io.github.remmerw.nott

internal class Peer(
    val id: ByteArray,
    val address: Address,
) {
    private val cachedLongKey: Long by lazy { id.toLong() }

    fun key(): Long = cachedLongKey

    override fun toString(): String = "Peer(address=$address)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Peer

        if (!id.contentEquals(other.id)) return false
        if (address != other.address) return false

        return true
    }
}
