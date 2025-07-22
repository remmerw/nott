package io.github.remmerw.nott

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.writeUShort
import java.net.InetAddress
import java.net.InetSocketAddress

internal data class Address(val data: ByteArray, val port: UShort) {

    fun encoded(): ByteArray {
        val buffer = Buffer()
        buffer.write(data)
        buffer.writeUShort(this.port)
        return buffer.readByteArray()
    }

    fun toInetSocketAddress(): InetSocketAddress {
        val address = InetAddress.getByAddress(data)
        return InetSocketAddress(address, port.toInt())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Address

        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}