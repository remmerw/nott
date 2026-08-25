package io.github.remmerw.nott

// The maximum UDP packet size for the BitTorrent Mainline DHT is typically
// limited by the Maximum Transmission Unit (MTU) of the network, and is
// often around 1400 bytes. This is smaller than the theoretical maximum
// of 65535 bytes for UDP packets.

internal const val UDP_PACKET = 1400

internal const val TID_LENGTH = 6


// DHT
internal const val RESPONSE_TIMEOUT = 3000
internal const val MAX_ENTRIES_PER_BUCKET: Int = 8
internal const val TOKEN_TIMEOUT: Int = 5 * 60 * 1000
internal const val MAX_DB_ENTRIES_PER_KEY: Int = 6000
internal const val MAX_PEERS_PER_ANNOUNCE: Int = 10
internal const val SHA1_HASH_LENGTH: Int = 20
internal const val ADDRESS_LENGTH_IPV6 = 16 + 2
internal const val ADDRESS_LENGTH_IPV4 = 4 + 2
internal const val NODE_ENTRY_LENGTH_IPV6 = ADDRESS_LENGTH_IPV6 + SHA1_HASH_LENGTH
internal const val NODE_ENTRY_LENGTH_IPV4 = ADDRESS_LENGTH_IPV4 + SHA1_HASH_LENGTH

