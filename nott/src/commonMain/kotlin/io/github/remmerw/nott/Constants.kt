// The maximum UDP packet size for the BitTorrent Mainline DHT is typically
// limited by the Maximum Transmission Unit (MTU) of the network, and is
// often around 1400 bytes. This is smaller than the theoretical maximum
// of 65535 bytes for UDP packets.

internal const val UDP_PACKET = 1400

internal const val TID_LENGTH = 6

// haven't seen it for a long time + timeout == evict sooner than pure timeout
// based threshold. e.g. for old entries that we haven't touched for a long time
internal const val OLD_AND_STALE_TIME = 15 * 60 * 1000

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

// -1 token per minute, 60 saturation, 30 threshold
// if we see more than 1 per minute then it'll take 30 minutes until an
// unsolicited request can go into a replacement bucket again
internal const val THROTTLE_INCREMENT: Long = 10

/*
* Verification Strategy:
*
* - trust incoming requests less than responses to outgoing requests
* - most outgoing requests will have an expected ID - expected ID may come from external nodes,
* so don't take it at face value
*  - if response does not match expected ID drop the packet for routing table accounting
* purposes without penalizing any existing routing table entry
* - map routing table entries to IP addresses
*  - verified responses trump unverified entries
*  - lookup all routing table entry for incoming messages based on IP address (not node ID!)
*  and ignore them if ID does not match
*  - also ignore if port changed
*  - drop, not just ignore, if we are sure that the incoming message is not fake
* (tid-verified response)
* - allow duplicate addresses for unverified entries
*  - scrub later when one becomes verified
* - never hand out unverified entries to other nodes
*
* other stuff to keep in mind:
*
* - non-reachable nodes may spam -> floods replacements -> makes it hard to get proper
* replacements without active lookups
*
*/
internal const val THROTTLE_SATURATION: Long = 60
internal const val THROTTLE_THRESHOLD: Long = 30