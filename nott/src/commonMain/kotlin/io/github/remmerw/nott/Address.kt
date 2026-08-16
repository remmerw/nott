package io.github.remmerw.nott

@Suppress("ArrayInDataClass")
internal data class Address(
    val address: ByteArray,
    val port: UShort){
}