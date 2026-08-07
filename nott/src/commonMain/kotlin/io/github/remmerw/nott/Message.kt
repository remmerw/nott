package io.github.remmerw.nott


import io.github.remmerw.buri.BEObject
import io.github.remmerw.buri.bencode
import io.github.remmerw.buri.encodeBencodeTo
import io.github.remmerw.buri.bencodeMap
import io.github.remmerw.buri.bencodeEof
import io.github.remmerw.buri.bencodeMapKey
import io.github.remmerw.buri.bencodeList
import kotlinx.io.Sink
import java.net.InetSocketAddress


internal sealed interface Message {
    val address: InetSocketAddress
    val id: ByteArray
    val tid: ByteArray
    fun encode(sink: Sink)
}

internal sealed interface Response : Message {
    val ip: ByteArray?
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
    override val address: InetSocketAddress,
    override val id: ByteArray,
    override val tid: ByteArray,
    override val ro: Boolean,
    val infoHash: ByteArray,
    val port: Int,
    val token: ByteArray,
    val name: ByteArray?,
) :
    Request {

    override fun encode(sink: Sink) { 
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
        if(name!= null){
             sink.bencodeMapKey(Names.NAME)
             sink.bencode(name)
        }
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencode(tid)
        if(ro){
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
    override val address: InetSocketAddress,
    override val id: ByteArray,
    override val tid: ByteArray,
    override val ip: ByteArray?
) : Response {

    override fun encode(sink: Sink) {
        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.R)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencode(tid)

        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.R)
        
        sink.bencodeEof() // end map
    }

}

@Suppress("ArrayInDataClass")
internal data class Error(
    override val address: InetSocketAddress,
    override val id: ByteArray,
    override val tid: ByteArray,
    val code: Int,
    val message: ByteArray
) : Message {

    override fun encode(sink: Sink) {
        sink.bencodeMap()

        sink.bencodeMapKey(Names.E)
        sink.bencodeList()
        sink.bencode(code)
        sink.bencode(message)
        sink.bencodeEof()

        sink.bencodeMapKey(Names.T)
        sink.bencode(tid)
        
        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.E)
        
        sink.bencodeEof()
    }

}


@Suppress("ArrayInDataClass")
internal data class FindNodeRequest(
    override val address: InetSocketAddress,
    override val id: ByteArray,
    override val tid: ByteArray,
    override val ro: Boolean,
    val target: ByteArray
) :
    Request {

    override fun encode(sink: Sink) {
        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.A)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        sink.bencodeMapKey(Names.TARGET)
        sink.bencode(target)
        sink.bencodeEof() // end map


        sink.bencodeMapKey(Names.T)
        sink.bencode(tid)
        
        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.Q)

        if(ro){
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
    override val address: InetSocketAddress,
    override val id: ByteArray,
    override val tid: ByteArray,
    override val ip: ByteArray?,
    override val nodes: List<Peer>,
    override val nodes6: List<Peer>
) : NodesResponse {

    override fun encode(sink: Sink) {
        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.R)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        if (nodes.isNotEmpty()) {
          sink.bencodeMapKey(Names.NODES)
          sink.bencode(writeBuckets(nodes))
        }
        if (nodes6.isNotEmpty()) {
          sink.bencodeMapKey(Names.NODES6)
          sink.bencode(writeBuckets(nodes6))
        }
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencode(tid)
        
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
    override val address: InetSocketAddress,
    override val id: ByteArray,
    override val tid: ByteArray,
    override val ro: Boolean,
    val infoHash: ByteArray
) :
    Request {

    override fun encode(sink: Sink) {

        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.A)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        sink.bencodeMapKey(Names.INFO_HASH)
        sink.bencode(infoHash)
        sink.bencodeEof() // end map

        
        sink.bencodeMapKey(Names.T)
        sink.bencode(tid)
        
        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.Q)

        if(ro){
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
    override val address: InetSocketAddress,
    override val id: ByteArray,
    override val tid: ByteArray,
    override val ip: ByteArray?,
    val token: ByteArray?,
    override val nodes: List<Peer>,
    override val nodes6: List<Peer>,
    val values: List<Address>
) : NodesResponse {


    override fun encode(sink: Sink) {
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
          sink.bencode(writeBuckets(nodes))
        }
        if (nodes6.isNotEmpty()) {
          sink.bencodeMapKey(Names.NODES6)
          sink.bencode(writeBuckets(nodes6))
        }
        
        if (values.isNotEmpty()) {
            sink.bencodeMapKey(Names.VALUES)
            sink.bencodeList()
            values.forEach { value ->
                sink.bencode(value.encoded())
            }
            sink.bencodeEof() // end list
        }
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencode(tid)

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
    override val address: InetSocketAddress,
    override val id: ByteArray,
    override val tid: ByteArray,
    override val ro: Boolean,
) : Request {

    override fun encode(sink: Sink) {
        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.A)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencode(tid)
        
        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.Q)

        if(ro){
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
    override val address: InetSocketAddress,
    override val id: ByteArray,
    override val tid: ByteArray,
    override val ip: ByteArray?
) : Response {

    override fun encode(sink: Sink) {
               
        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.R)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencode(tid)

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
    override val address: InetSocketAddress,
    override val id: ByteArray,
    override val tid: ByteArray,
    override val ro: Boolean,
    val token: ByteArray,
    val v: BEObject,
    val cas: Long?,
    val k: ByteArray?,
    val salt: ByteArray?,
    val seq: Long?,
    val sig: ByteArray?
) :
    Request {

    override fun encode(sink: Sink) {
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
        sink.bencode(tid)
        
        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.Q)

        if(ro){
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
    override val address: InetSocketAddress,
    override val id: ByteArray,
    override val tid: ByteArray,
    override val ip: ByteArray?
) : Response {

    override fun encode(sink: Sink) {
        sink.bencodeMap() // new map

        sink.bencodeMapKey(Names.R)
        sink.bencodeMap() // new map
        sink.bencodeMapKey(Names.ID)
        sink.bencode(id)
        sink.bencodeEof() // end map

        sink.bencodeMapKey(Names.T)
        sink.bencode(tid)

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
    override val address: InetSocketAddress,
    override val id: ByteArray,
    override val tid: ByteArray,
    override val ro: Boolean,
    val target: ByteArray,
    val seq: Long?
) :
    Request {

    override fun encode(sink: Sink) {
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
        sink.bencode(tid)
        
        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.Q)

        if(ro){
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
    override val address: InetSocketAddress,
    override val id: ByteArray,
    override val tid: ByteArray,
    override val ip: ByteArray?,
    val token: ByteArray?,
    override val nodes: List<Peer>,
    override val nodes6: List<Peer>,
    val v: BEObject?,
    val k: ByteArray?,
    val seq: Long?,
    val sig: ByteArray?
) : NodesResponse {


    override fun encode(sink: Sink) {
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
          sink.bencode(writeBuckets(nodes))
        }
        if (nodes6.isNotEmpty()) {
          sink.bencodeMapKey(Names.NODES6)
          sink.bencode(writeBuckets(nodes6))
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
        sink.bencode(tid)

        sink.bencodeMapKey(Names.Y)
        sink.bencode(Names.R)


        sink.bencodeEof() // end map

       
    }
}