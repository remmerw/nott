package io.github.remmerw.nott

@Suppress("ArrayInDataClass")
internal data class Address(
    val address: ByteArray,
    val port: UShort,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Address

        if (!address.contentEquals(other.address)) return false
        if (port != other.port) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.contentHashCode()
        result = 31 * result + port.hashCode()
        return result
    }
}
