package io.github.remmerw.nott

import java.net.InetAddress
import java.net.InetSocketAddress

sealed interface Address {
    val address: ByteArray
    val port: UShort

    fun inetAddress(): InetAddress

    fun inetSocketAddress(): InetSocketAddress
}

@Suppress("ArrayInDataClass")
internal data class InetAddress(
    val inetAddress: InetAddress,
    override val address: ByteArray,
    override val port: UShort,
) : Address {
    private val inetSocketAddress: InetSocketAddress by lazy {
        InetSocketAddress(inetAddress(), port.toInt())
    }

    override fun inetAddress(): InetAddress = inetAddress

    override fun inetSocketAddress(): InetSocketAddress = inetSocketAddress

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawAddress

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
internal data class IosAddress(
    val ios: InetSocketAddress,
    override val address: ByteArray,
    override val port: UShort,
) : Address {
    override fun inetAddress(): InetAddress = ios.address

    override fun inetSocketAddress(): InetSocketAddress = ios

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawAddress

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
    override val address: ByteArray,
    override val port: UShort,
) : Address {
    private val inetAddress: InetAddress by lazy { InetAddress.getByAddress(address) }

    private val inetSocketAddress: InetSocketAddress by lazy {
        InetSocketAddress(inetAddress(), port.toInt())
    }

    override fun inetAddress(): InetAddress = inetAddress

    override fun inetSocketAddress(): InetSocketAddress = inetSocketAddress

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawAddress

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

fun createAddress(
    inet: InetAddress,
    port: Int,
): Address = InetAddress(inet, inet.address, port.toUShort())

fun createAddress(iso: InetSocketAddress): Address = IosAddress(iso, iso.address.address, iso.port.toUShort())

fun createAddress(
    address: ByteArray,
    port: UShort,
): Address = RawAddress(address, port)
