    

internal data class DistanceOrder(
        val target: ByteArray,
    ) : Comparator<Peer> {
        override fun compare(
            a: Peer,
            b: Peer,
        ): Int = threeWayDistance(target, a.id, b.id)
    }