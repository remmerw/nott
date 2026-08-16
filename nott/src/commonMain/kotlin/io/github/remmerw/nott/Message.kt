package io.github.remmerw.nott

import io.github.remmerw.buri.Buffer
import io.github.remmerw.buri.bencode
import io.github.remmerw.buri.bencodeArray
import io.github.remmerw.buri.bencodeArrayData
import io.github.remmerw.buri.bencodeEof
import io.github.remmerw.buri.bencodeList
import io.github.remmerw.buri.bencodeMap
import io.github.remmerw.buri.bencodeMapKey

internal sealed interface Message {
    val address: Address
    val id: ByteArray
    val tid: Long

    fun encode(sink: Buffer)
}

internal sealed interface Response : Message {
    val ip: Address?
}

internal sealed interface NodesResponse : Response {
    val nodes: List<Peer>
    val nodes6: List<Peer>
}

internal sealed interface Request : Message {
    val ro: Boolean
}

@Suppress("ArrayInDataClass")
internal data class AnnounceRequest(
    override val address: Address,
    override val id: ByteArray,
    override val tid: Long,
    override val ro: Boolean,
    val infoHash: ByteArray,
    val port: Int,
    val token: ByteArray,
    val name: ByteArray?,
) : Request {
    override fun encode(sink: Buffer) {
        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.A)

        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        sink.bencodeMapKey(Names.INFO_HASH)
        sink.bencode(infoHash)
        sink.bencodeMapKey(Names.PORT)
        sink.bencode(port)
        sink.bencodeMapKey(Names.TOKEN)
        sink.bencode(token)
        if (name != null) {
            sink.bencodeMapKey(Names.NAME)
            sink.bencode(name)
        }
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencodeTid(tid)
        if (ro) {
            sink.bencodeMapKey(Names.RO)
            sink.bencode(1)
        }
        sink.bencodeMapKey(Names.Q)
        sink.bencode(Names.ANNOUNCE_PEER)
        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.Q)

        sink.bencodeEof() // end map
    }
}

@Suppress("ArrayInDataClass")
internal data class AnnounceResponse(
    override val address: Address,
    override val id: ByteArray,
    override val tid: Long,
    override val ip: Address?,
) : Response {
    override fun encode(sink: Buffer) {
        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.R)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencodeTid(tid)
        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.R)

        if (ip != null) {
            sink.bencodeMapKey(Names.IP)
            sink.bencode(ip)
        }
        sink.bencodeEof() // end map
    }
}

@Suppress("ArrayInDataClass")
internal data class Error(
    override val address: Address,
    override val id: ByteArray,
    override val tid: Long,
    val code: Int,
    val message: ByteArray,
) : Message {
    override fun encode(sink: Buffer) {
        sink.bencodeMap()

        sink.bencodeMapKey(Names.E)
        sink.bencodeList()
        sink.bencode(code)
        sink.bencode(message)
        sink.bencodeEof()

        sink.bencodeMapKey(Names.T)
        sink.bencodeTid(tid)
        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.E)

        sink.bencodeEof()
    }
}

@Suppress("ArrayInDataClass")
internal data class FindNodeRequest(
    override val address: Address,
    override val id: ByteArray,
    override val tid: Long,
    override val ro: Boolean,
    val target: ByteArray,
) : Request {
    override fun encode(sink: Buffer) {
        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.A)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        sink.bencodeMapKey(Names.TARGET)
        sink.bencode(target)
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencodeTid(tid)

        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.Q)

        if (ro) {
            sink.bencodeMapKey(Names.RO)
            sink.bencode(1)
        }

        sink.bencodeMapKey(Names.Q)
        sink.bencode(Names.FIND_NODE)

        sink.bencodeEof() // end map
    }
}

@Suppress("ArrayInDataClass")
internal data class FindNodeResponse(
    override val address: Address,
    override val id: ByteArray,
    override val tid: Long,
    override val ip: Address?,
    override val nodes: List<Peer>,
    override val nodes6: List<Peer>,
) : NodesResponse {
    override fun encode(sink: Buffer) {
        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.R)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        if (nodes.isNotEmpty()) {
            sink.bencodeMapKey(Names.NODES)
            sink.bencodePeers(nodes, NODE_ENTRY_LENGTH_IPV4)
        }
        if (nodes6.isNotEmpty()) {
            sink.bencodeMapKey(Names.NODES6)
            sink.bencodePeers(nodes6, NODE_ENTRY_LENGTH_IPV6)
        }
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencodeTid(tid)

        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.R)

        if (ip != null) {
            sink.bencodeMapKey(Names.IP)
            sink.bencode(ip)
        }

        sink.bencodeEof() // end map
    }
}

@Suppress("ArrayInDataClass")
internal data class GetPeersRequest(
    override val address: Address,
    override val id: ByteArray,
    override val tid: Long,
    override val ro: Boolean,
    val infoHash: ByteArray,
) : Request {
    override fun encode(sink: Buffer) {
        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.A)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        sink.bencodeMapKey(Names.INFO_HASH)
        sink.bencode(infoHash)
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencodeTid(tid)

        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.Q)

        if (ro) {
            sink.bencodeMapKey(Names.RO)
            sink.bencode(1)
        }

        sink.bencodeMapKey(Names.Q)
        sink.bencode(Names.GET_PEERS)

        sink.bencodeEof()
    }
}

@Suppress("ArrayInDataClass")
internal data class GetPeersResponse(
    override val address: Address,
    override val id: ByteArray,
    override val tid: Long,
    override val ip: Address?,
    val token: ByteArray?,
    override val nodes: List<Peer>,
    override val nodes6: List<Peer>,
    val values: List<Address>,
) : NodesResponse {
    override fun encode(sink: Buffer) {
        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.R)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        if (token != null) {
            sink.bencodeMapKey(Names.TOKEN)
            sink.bencode(token)
        }
        if (nodes.isNotEmpty()) {
            sink.bencodeMapKey(Names.NODES)
            sink.bencodePeers(nodes, NODE_ENTRY_LENGTH_IPV4)
        }
        if (nodes6.isNotEmpty()) {
            sink.bencodeMapKey(Names.NODES6)
            sink.bencodePeers(nodes6, NODE_ENTRY_LENGTH_IPV6)
        }
        if (values.isNotEmpty()) {
            sink.bencodeMapKey(Names.VALUES)
            sink.bencodeList()
            for (value in values) {
                sink.bencode(value)
            }
            sink.bencodeEof() // end list
        }
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencodeTid(tid)

        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.R)

        if (ip != null) {
            sink.bencodeMapKey(Names.IP)
            sink.bencode(ip)
        }

        sink.bencodeEof() // end map
    }
}

@Suppress("ArrayInDataClass")
internal data class PingRequest(
    override val address: Address,
    override val id: ByteArray,
    override val tid: Long,
    override val ro: Boolean,
) : Request {
    override fun encode(sink: Buffer) {
        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.A)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencodeTid(tid)

        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.Q)

        if (ro) {
            sink.bencodeMapKey(Names.RO)
            sink.bencode(1)
        }

        sink.bencodeMapKey(Names.Q)
        sink.bencode(Names.PING)

        sink.bencodeEof()
    }
}

@Suppress("ArrayInDataClass")
internal data class PingResponse(
    override val address: Address,
    override val id: ByteArray,
    override val tid: Long,
    override val ip: Address?,
) : Response {
    override fun encode(sink: Buffer) {
        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.R)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencodeTid(tid)

        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.R)

        if (ip != null) {
            sink.bencodeMapKey(Names.IP)
            sink.bencode(ip)
        }

        sink.bencodeEof() // end map
    }
}

@Suppress("ArrayInDataClass")
internal data class PutRequest(
    override val address: Address,
    override val id: ByteArray,
    override val tid: Long,
    override val ro: Boolean,
    val token: ByteArray,
    val v: ByteArray,
    val cas: Long?,
    val k: ByteArray?,
    val salt: ByteArray?,
    val seq: Long?,
    val sig: ByteArray?,
) : Request {
    override fun encode(sink: Buffer) {
        sink.bencodeMap()

        sink.bencodeMapKey(Names.A)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        sink.bencodeMapKey(Names.V)
        sink.bencode(v)
        sink.bencodeMapKey(Names.TOKEN)
        sink.bencode(token)
        if (cas != null) {
            sink.bencodeMapKey(Names.CAS)
            sink.bencode(cas)
        }
        if (k != null) {
            sink.bencodeMapKey(Names.K)
            sink.bencode(k)
        }
        if (salt != null) {
            sink.bencodeMapKey(Names.SALT)
            sink.bencode(salt)
        }
        if (seq != null) {
            sink.bencodeMapKey(Names.SEQ)
            sink.bencode(seq)
        }
        if (sig != null) {
            sink.bencodeMapKey(Names.SIG)
            sink.bencode(sig)
        }
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencodeTid(tid)

        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.Q)

        if (ro) {
            sink.bencodeMapKey(Names.RO)
            sink.bencode(1)
        }

        sink.bencodeMapKey(Names.Q)
        sink.bencode(Names.PUT)

        sink.bencodeEof() // end map
    }
}

@Suppress("ArrayInDataClass")
internal data class PutResponse(
    override val address: Address,
    override val id: ByteArray,
    override val tid: Long,
    override val ip: Address?,
) : Response {
    override fun encode(sink: Buffer) {
        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.R)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencodeTid(tid)
        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.R)

        if (ip != null) {
            sink.bencodeMapKey(Names.IP)
            sink.bencode(ip)
        }

        sink.bencodeEof() // end map
    }
}

@Suppress("ArrayInDataClass")
internal data class GetRequest(
    override val address: Address,
    override val id: ByteArray,
    override val tid: Long,
    override val ro: Boolean,
    val target: ByteArray,
    val seq: Long?,
) : Request {
    override fun encode(sink: Buffer) {
        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.A)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        sink.bencodeMapKey(Names.TARGET)
        sink.bencode(target)
        if (seq != null) {
            sink.bencodeMapKey(Names.SEQ)
            sink.bencode(seq)
        }
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencodeTid(tid)

        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.Q)

        if (ro) {
            sink.bencodeMapKey(Names.RO)
            sink.bencode(1)
        }

        sink.bencodeMapKey(Names.Q)
        sink.bencode(Names.GET)

        sink.bencodeEof()
    }
}

@Suppress("ArrayInDataClass")
internal data class GetResponse(
    override val address: Address,
    override val id: ByteArray,
    override val tid: Long,
    override val ip: Address?,
    val token: ByteArray?,
    override val nodes: List<Peer>,
    override val nodes6: List<Peer>,
    val v: ByteArray?,
    val k: ByteArray?,
    val seq: Long?,
    val sig: ByteArray?,
) : NodesResponse {
    override fun encode(sink: Buffer) {
        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.R)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        if (token != null) {
            sink.bencodeMapKey(Names.TOKEN)
            sink.bencode(token)
        }
        if (nodes.isNotEmpty()) {
            sink.bencodeMapKey(Names.NODES)
            sink.bencodePeers(nodes, NODE_ENTRY_LENGTH_IPV4)
        }
        if (nodes6.isNotEmpty()) {
            sink.bencodeMapKey(Names.NODES6)
            sink.bencodePeers(nodes6, NODE_ENTRY_LENGTH_IPV6)
        }
        if (v != null) {
            sink.bencodeMapKey(Names.V)
            sink.bencode(v)
        }
        if (k != null) {
            sink.bencodeMapKey(Names.K)
            sink.bencode(k)
        }
        if (seq != null) {
            sink.bencodeMapKey(Names.SEQ)
            sink.bencode(seq)
        }
        if (sig != null) {
            sink.bencodeMapKey(Names.SIG)
            sink.bencode(sig)
        }

        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)

        sink.bencodeTid(tid)
        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.R)

        sink.bencodeEof() // end map
    }
}

internal fun Buffer.bencodePeers(
    list: List<Peer>,
    size: Int,
) {
    this.bencodeArray(list.size * size)
    list.forEach { peer: Peer ->
        this.bencodeArrayData(peer.id)
        this.bencodeArrayData(peer.address.address)
        this.bencodeArrayData(peer.address.port)
    }
}


internal fun Buffer.bencodeTid(value: Long) {
    this.bencodeArray(TID_LENGTH)

    this.bencodeArrayData(((value shr 40) and 0xFFL).toByte())
    this.bencodeArrayData(((value shr 32) and 0xFFL).toByte())
    this.bencodeArrayData(((value shr 24) and 0xFFL).toByte())
    this.bencodeArrayData(((value shr 16) and 0xFFL).toByte())
    this.bencodeArrayData(((value shr 8) and 0xFFL).toByte())
    this.bencodeArrayData((value and 0xFFL).toByte())
}

internal fun Buffer.bencode(address: Address) {
    val data = address.address

    this.bencodeArray(data.size + 2)

    this.bencodeArrayData(data)
    this.bencodeArrayData(address.port)
}