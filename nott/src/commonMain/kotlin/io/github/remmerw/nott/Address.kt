package io.github.remmerw.nott

import java.net.InetAddress
import java.net.InetSocketAddress

sealed interface Address {
    val address: Address
    val port: UShort 

    fun inetAddress(): InetAddress
    fun inetSocketAddress(): InetSocketAddress 

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
@Suppress("ArrayInDataClass")
internal data class RawAddress(
    val address: ByteArray,
    val port: UShort,
) : Address {
    private val inetAddress: InetAddress by lazy { InetAddress.getByAddress(address) }
    
    private val inetSocketAddress: InetSocketAddress by lazy { InetSocketAddress(inetAddress(), port.toInt()
     }

    fun inetAddress(): InetAddress = inetAddress

    fun inetSocketAddress(): InetSocketAddress = inetSocketAddress 
}

fun createAddress(iso: InetSocketAddress): Address { 
    return RawAddress(iso.address.address, iso.port.toUShort()
}

fun createAddress(address: ByteArray, port: UShort): Address {
    return RawAddress(address,port)
}
