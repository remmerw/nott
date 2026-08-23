package io.github.remmerw.nott

import io.github.remmerw.buri.BEInteger
import io.github.remmerw.buri.BEList
import io.github.remmerw.buri.BEMap
import io.github.remmerw.buri.BEObject
import io.github.remmerw.buri.BEString

private fun parseError(
    address: Address,
    map: Map<String, BEObject>,
): Message {
    val error = map[Names.E]

    var errorCode = 0
    var errorMsg: String? = null

    if (error is BEString) {
        errorMsg = stringGet(error)
    } else if (error is BEList) {
        val errmap = error.toList()
        try {
            errorCode = (errmap[0] as BEInteger).toInt()
            errorMsg = stringGet(errmap[1])
        } catch (throwable: Throwable) {
            debug(throwable)
        }
    }
    if (errorMsg == null) errorMsg = ""

    var tid = arrayGet(map[Names.T])
    if (tid == null) {
        tid = ByteArray(TID_LENGTH)
    }
    var id = arrayGet(map[Names.ID])
    if (id == null || id.size != SHA1_HASH_LENGTH) {
        id = ByteArray(SHA1_HASH_LENGTH)
    }

    return Error(
        address = address,
        id = id,
        tid = tid.toLong(TID_LENGTH),
        code = errorCode,
        message = errorMsg.encodeToByteArray(),
    )
}

private fun extractNodes6(args: Map<String, BEObject>): List<Peer> {
    val raw = arrayGet(args[Names.NODES6]) ?: return emptyList()
    require(raw.size % NODE_ENTRY_LENGTH_IPV6 == 0) {
        "expected length to be a multiple of " +
            NODE_ENTRY_LENGTH_IPV6 + ", received " + raw.size
    }
    return readBuckets(raw, ADDRESS_LENGTH_IPV6)
}

private fun extractNodes(args: Map<String, BEObject>): List<Peer> {
    val raw = arrayGet(args[Names.NODES]) ?: return emptyList()
    require(raw.size % NODE_ENTRY_LENGTH_IPV4 == 0) {
        "expected length to be a multiple of " +
            NODE_ENTRY_LENGTH_IPV4 + ", received " + raw.size
    }
    return readBuckets(raw, ADDRESS_LENGTH_IPV4)
}

internal fun readBuckets(
    src: ByteArray,
    length: Int,
): List<Peer> {
    val capacity: Int = (src.size / (length + SHA1_HASH_LENGTH))
    val result = ArrayList<Peer>(capacity)

    var offset = 0
    while (offset < src.size) {
        val rawId = src.copyOfRange(offset, offset + SHA1_HASH_LENGTH)
        offset += SHA1_HASH_LENGTH

        val raw = src.copyOfRange(offset, offset + (length - 2))
        offset += (length - 2)

        // Port auslesen (Big-Endian)
        val port =
            (
                ((src[offset].toInt() and 0xFF) shl 8) or
                    (src[offset + 1].toInt() and 0xFF)
            ).toUShort()
        offset += 2

        if (port > 0.toUShort() && port <= 65535.toUShort()) {
            try {
                val peer =
                    Peer(
                        rawId,
                        createAddress(
                            raw,
                            port,
                        ),
                    )
                result.add(peer)
            } catch (throwable: Throwable) {
                debug(throwable)
            }
        }
    }
    return result
}

internal fun parseMessage(
    address: Address,
    map: Map<String, BEObject>,
    tidMapper: (Long) -> (Request?),
): Message? {
    val msgType = stringGet(map[Names.Y])

    if (msgType == null) {
        debug("message type (y) missing")
        return null
    }

    return when (msgType) {
        Names.Q -> {
            parseRequest(address, map)
        }

        Names.R -> {
            parseResponse(address, map, tidMapper)
        }

        Names.E -> {
            parseError(address, map)
        }

        else -> {
            debug("unknown RPC type (y=$msgType)")
            null
        }
    }
}

private fun parseRequest(
    address: Address,
    map: Map<String, BEObject>,
): Message? {
    val ro = roGet(map[Names.RO])
    val tid = arrayGet(map[Names.T])
    checkNotNull(tid) { "missing transaction ID in request" }
    require(tid.isNotEmpty()) { "zero-length transaction ID in request" }
    require(tid.size <= TID_LENGTH) { "invalid transaction ID length " + tid.size }

    val root = map[Names.A] as BEMap
    val args = root.toMap()

    val id = arrayGet(args[Names.ID])
    checkNotNull(id) { "missing id" }
    require(id.size == SHA1_HASH_LENGTH) { "invalid node id" }

    val requestMethod = stringGet(map[Names.Q])

    return when (requestMethod) {
        Names.PING ->
            PingRequest(
                address = address,
                id = id,
                tid = tid.toLong(TID_LENGTH),
                ro = ro,
            )

        Names.FIND_NODE -> {
            val target = arrayGet(args[Names.TARGET])

            if (target == null) {
                debug("missing/invalid target key in request")
                return null
            }

            if (target.size != SHA1_HASH_LENGTH) {
                debug("invalid target key in request")
                return null
            }

            FindNodeRequest(
                address = address,
                id = id,
                tid = tid.toLong(TID_LENGTH),
                ro = ro,
                target = target,
            )
        }

        Names.GET_PEERS -> {
            val infoHash = arrayGet(args[Names.INFO_HASH])

            if (infoHash == null) {
                debug("missing/invalid target key in request")
                return null
            }

            if (infoHash.size != SHA1_HASH_LENGTH) {
                debug("invalid target key in request")
                return null
            }
            GetPeersRequest(
                address = address,
                id = id,
                tid = tid.toLong(TID_LENGTH),
                ro = ro,
                infoHash = infoHash,
            )
        }

        Names.GET -> {
            val target = arrayGet(args[Names.TARGET])

            if (target == null) {
                debug("missing/invalid target key in request")
                return null
            }

            if (target.size != SHA1_HASH_LENGTH) {
                debug("invalid target key in request")
                return null
            }

            val seq = longGet(args[Names.SEQ])

            GetRequest(
                address = address,
                id = id,
                tid = tid.toLong(TID_LENGTH),
                ro = ro,
                target = target,
                seq = seq,
            )
        }

        Names.PUT -> {
            val token = arrayGet(args[Names.TOKEN])

            require(token != null) {
                "missing or invalid mandatory arguments (token) for announce"
            }

            require(!token.isEmpty()) {
                "zero-length token in announce_peer request. see BEP33 for reasons why " +
                    "tokens might not have been issued by get_peers response"
            }

            val v = arrayGet(args[Names.V])

            require(v != null) {
                "missing or invalid mandatory arguments (v) for put"
            }

            val cas = longGet(args[Names.CAS])
            val k = arrayGet(args[Names.K])
            val salt = arrayGet(args[Names.SALT])
            val seq = longGet(args[Names.SEQ])
            val sig = arrayGet(args[Names.SIG])

            PutRequest(
                address = address,
                id = id,
                tid = tid.toLong(TID_LENGTH),
                ro = ro,
                token = token,
                v = v,
                cas = cas,
                k = k,
                salt = salt,
                seq = seq,
                sig = sig,
            )
        }

        Names.ANNOUNCE_PEER -> {
            val infoHash = arrayGet(args[Names.INFO_HASH])
            checkNotNull(infoHash) {
                "missing info_hash for announce"
            }
            require(infoHash.size == SHA1_HASH_LENGTH) { "invalid info_hash" }

            val port = longGet(args[Names.PORT])
            checkNotNull(port) { "missing port for announce" }
            require(port in 1..65535) { "invalid port" }

            val token = arrayGet(args[Names.TOKEN])

            require(token != null) {
                "missing or invalid mandatory arguments (info_hash, port, token) for announce"
            }

            require(!token.isEmpty()) {
                "zero-length token in announce_peer request. see BEP33 for reasons why " +
                    "tokens might not have been issued by get_peers response"
            }

            val name = arrayGet(args[Names.NAME])
            AnnounceRequest(
                address = address,
                id = id,
                tid = tid.toLong(TID_LENGTH),
                ro = ro,
                infoHash = infoHash,
                port = port.toInt(),
                token = token,
                name = name,
            )
        }

        else -> {
            debug("method unknown in request")
            null
        }
    }
}

private fun parseResponse(
    address: Address,
    map: Map<String, BEObject>,
    tidMapper: (Long) -> (Request?),
): Message? {
    val tid = arrayGet(map[Names.T])

    checkNotNull(tid) { "missing transaction ID in request" }
    require(tid.isNotEmpty()) { "zero-length transaction ID in request" }
    require(tid.size <= TID_LENGTH) { "invalid transaction ID length " + tid.size }

    // responses don't have explicit methods, need to match them to a request to figure that one out
    val ltid = tid.toLong(TID_LENGTH)
    val request = tidMapper.invoke(ltid)
    if (request == null) {
        debug("response does not have a known request (tid)")
        return null
    }
    return parseResponse(address, map, request, ltid)
}

private fun parseResponse(
    address: Address,
    map: Map<String, BEObject>,
    request: Request,
    tid: Long,
): Message? {
    val inner = map[Names.R]
    if (inner !is BEMap) {
        return null
    }
    val args = inner.toMap()

    val id = arrayGet(args[Names.ID])
    require(id != null) { "mandatory parameter 'id' missing" }
    require(id.size == SHA1_HASH_LENGTH) { "invalid or missing origin ID" }

    val ip = null // not yet supported  inetGet(map[Names.IP])

    val msg: Message

    when (request) {
        is PingRequest -> msg = PingResponse(address, id, tid, ip)
        is PutRequest -> msg = PutResponse(address, id, tid, ip)
        is GetRequest -> {
            val token = arrayGet(args[Names.TOKEN])
            val nodes6 = extractNodes6(args)
            val nodes = extractNodes(args)
            val v = arrayGet(args[Names.V])
            val k = arrayGet(args[Names.K])
            val sec = longGet(args[Names.SEQ])
            val sig = arrayGet(args[Names.SIG])
            return GetResponse(
                address,
                id,
                tid,
                ip,
                token,
                nodes,
                nodes6,
                v,
                k,
                sec,
                sig,
            )
        }

        is AnnounceRequest -> msg = AnnounceResponse(address, id, tid, ip)
        is FindNodeRequest -> {
            val nodes6 = extractNodes6(args)
            val nodes = extractNodes(args)
            msg = FindNodeResponse(address, id, tid, ip, nodes, nodes6)
        }

        is GetPeersRequest -> {
            val token = arrayGet(args[Names.TOKEN])
            val nodes6 = extractNodes6(args)
            val nodes = extractNodes(args)
            val addresses: MutableList<Address> = mutableListOf()

            var vals: List<ByteArray> = listOf()
            val values = args[Names.VALUES]
            if (values != null) {
                vals =
                    (values as BEList).toList().map {
                        (it as BEString).toByteArray()
                    }
            }

            if (vals.isNotEmpty()) {
                for (i in vals.indices) {
                    // only accept ipv4 or ipv6 for now
                    val length = vals[i].size
                    when (length) {
                        ADDRESS_LENGTH_IPV4 -> {
                            val buffer = vals[i]

                            val address: ByteArray = buffer.copyOfRange(0, 4)
                            val port: UShort =
                                (
                                    (
                                        buffer[4]
                                            .toInt() and 0xFF
                                    ) shl 8 or (buffer[5].toInt() and 0xFF)
                                ).toUShort()

                            if (port > 0.toUShort() && port <= 65535.toUShort()) {
                                addresses.add(
                                    createAddress(
                                        address,
                                        port,
                                    ),
                                )
                            }
                        }

                        ADDRESS_LENGTH_IPV6 -> {
                            val buffer = vals[i]

                            val address: ByteArray = buffer.copyOfRange(0, 16)
                            val port: UShort =
                                (
                                    (
                                        buffer[16]
                                            .toInt() and 0xFF
                                    ) shl 8 or (buffer[17].toInt() and 0xFF)
                                ).toUShort()

                            if (port > 0.toUShort() && port <= 65535.toUShort()) {
                                addresses.add(
                                    createAddress(
                                        address,
                                        port,
                                    ),
                                )
                            }
                        }

                        else -> {
                            debug("not accepted address length (just ignore)")
                            return null
                        }
                    }
                }
            }
            return GetPeersResponse(
                address,
                id,
                tid,
                ip,
                token,
                nodes,
                nodes6,
                addresses,
            )
        }
    }

    return msg
}

internal fun stringGet(beObject: BEObject?): String? {
    if (beObject == null) {
        return null
    }

    if (beObject is BEString) {
        return beObject.toString()
    }
    return null
}

internal fun arrayGet(beObject: BEObject?): ByteArray? {
    if (beObject == null) {
        return null
    }

    if (beObject is BEString) {
        return beObject.toByteArray()
    }
    return null
}

internal fun inetGet(beObject: BEObject?): Address? {
    if (beObject == null) {
        return null
    }

    if (beObject is BEString) {
        val data = beObject.toByteArray()
        val length = data.size - 2
        val address = data.copyOfRange(0, length)
        val port: UShort =
            (
                (
                    data[length]
                        .toInt() and 0xFF
                ) shl 8 or (data[length + 1].toInt() and 0xFF)
            ).toUShort()

        return createAddress(
            address,
            port,
        )
    }
    return null
}

internal fun longGet(beObject: BEObject?): Long? {
    if (beObject == null) {
        return null
    }
    if (beObject is BEInteger) {
        return beObject.toLong()
    }
    return null
}

internal fun roGet(beObject: BEObject?): Boolean {
    if (beObject == null) {
        return false
    }
    if (beObject is BEInteger) {
        return beObject.toInt() == 1
    }
    return false
}

internal const val GENERIC_ERROR = 201
internal const val SERVER_ERROR = 202
internal const val PROTOCOL_ERROR = 203
