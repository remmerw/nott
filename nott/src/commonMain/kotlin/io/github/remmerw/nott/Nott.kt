package io.github.remmerw.nott

import io.github.remmerw.buri.BEMap
import io.github.remmerw.buri.BEObject
import io.github.remmerw.buri.BEReader
import io.github.remmerw.buri.decodeBencode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kotlincrypto.hash.sha1.SHA1
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class Nott(
    val nodeId: ByteArray,
    val readOnlyState: Boolean = true,
    val bootstrap: Set<Address> = defaultBootstrap(),
) {
    private val requestCalls: MutableMap<Long, Call> = ConcurrentHashMap()
    private val database: Database = Database()
    private val tokenManager = TokenManager()
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var channel = DatagramChannel.open()
    private val routingTable = RoutingTable()
    private val sending = ByteBuffer.allocateDirect(UDP_PACKET)
    private val received = ByteBuffer.allocateDirect(UDP_PACKET)

    fun port(): Int = channel.socket().getLocalPort()

    suspend fun bootstrap() {
        try {
            bootstrap.forEach { address: Address ->
                ping(address, null)
            }
        } catch (throwable: Throwable) {
            debug(throwable)
        }
    }

    fun startup() {
        channel.bind(InetSocketAddress(0))
        scope.launch {
            try {
                while (isActive) {
                    received.clear()
                    val address = channel.receive(received) as InetSocketAddress
                    received.flip()

                    val inet = createAddress(address)

                    val length = received.remaining()

                    // * no conceivable DHT message is smaller than 10 bytes
                    // * port 0 is reserved
                    // -> immediately discard junk on the read loop, don't even allocate a
                    // buffer for it
                    if (length < 10 || inet.port.toInt() == 0) continue

                    val reader = BEReader(received)
                    handlePacket(reader, inet)
                }
            } catch (throwable: Throwable) {
                debug(throwable)
            }
        }
    }

    internal fun findPeerById(id: ByteArray): Peer? = routingTable.findPeerById(id)

    internal suspend fun doRequestCall(
        request: Request,
        expectedId: ByteArray?,
    ): Call {
        val call = Call(request, expectedId)
        requestCalls[request.tid] = call
        send(request, call)
        return call
    }

    internal fun closestPeers(
        key: ByteArray,
        take: Int,
    ): List<Peer> = routingTable.closestPeers(key, take)

    private suspend fun send(
        message: Message,
        associatedCall: Call?,
    ) {
        mutex.withLock {
            sending.clear()

            message.encode(sending)
            sending.flip()

            val address = message.address

            val ios = address.inetSocketAddress()

            try {
                channel.send(sending, ios)

                associatedCall?.hasSend()
            } catch (throwable: Throwable) {
                debug(throwable)

                if (associatedCall != null) {
                    timeout(associatedCall)
                }
            }
        }
    }

    fun shutdown() {
        try {
            scope.cancel()
        } catch (throwable: Throwable) {
            debug(throwable)
        }

        try {
            channel.close()
        } catch (throwable: Throwable) {
            debug(throwable)
        }
    }

    internal fun timeout(call: Call) {
        call.injectError()
        requestCalls.remove(call.request.tid)

        // don't time out anything if we don't have a connection
        if (call.expectedID != null) {
            routingTable.remove(
                call.expectedID,
            )
        }
    }

    internal suspend fun ping(request: PingRequest) {
        val rsp =
            PingResponse(
                address = request.address,
                id = nodeId,
                tid = request.tid,
                ip = request.address,
            )

        sendMessage(rsp)
    }

    internal suspend fun findNode(request: FindNodeRequest) {
        val entries = routingTable.closestPeers(request.target, 8)
        val (ipv4Peers, ipv6Peers) =
            entries
                .partition { it.address.address.size == 4 }

        val response =
            FindNodeResponse(
                address = request.address,
                id = nodeId,
                tid = request.tid,
                ip = request.address,
                nodes = ipv4Peers,
                nodes6 = ipv6Peers,
            )

        sendMessage(response)
    }

    internal suspend fun getPeers(request: GetPeersRequest) {
        val values = database.sample(request.infoHash, MAX_PEERS_PER_ANNOUNCE)

        // generate a token
        var token: ByteArray? = null

        if (database.insertForKeyAllowed(request.infoHash)) {
            token =
                tokenManager.generateToken(
                    request.id,
                    request.address.address,
                    request.address.port,
                    request.infoHash,
                )
        }

        val entries = routingTable.closestPeers(request.infoHash, 8)
        val (ipv4Peers, ipv6Peers) =
            entries
                .partition { it.address.address.size == 4 }

        val resp =
            GetPeersResponse(
                address = request.address,
                id = nodeId,
                tid = request.tid,
                ip = request.address,
                token = token,
                nodes = ipv4Peers,
                nodes6 = ipv6Peers,
                values = values,
            )

        sendMessage(resp)
    }

    internal suspend fun get(request: GetRequest) {
        // generate a token
        var token: ByteArray? = null
        if (database.insertForKeyAllowed(request.target)) {
            token =
                tokenManager.generateToken(
                    request.id,
                    request.address.address,
                    request.address.port,
                    request.target,
                )
        }

        val entries = routingTable.closestPeers(request.target, 8)
        val (ipv4Peers, ipv6Peers) =
            entries
                .partition { it.address.address.size == 4 }

        val resp =
            GetResponse(
                address = request.address,
                id = nodeId,
                tid = request.tid,
                ip = request.address,
                token = token,
                nodes = ipv4Peers,
                nodes6 = ipv6Peers,
                null,
                null,
                null,
                null, // TODO [Low Priority]
            )

        sendMessage(resp)
    }

    internal suspend fun put(request: PutRequest) {
        // first check if the token is OK
        if (!tokenManager.checkToken(
                request.token,
                request.id,
                request.address.address,
                request.address.port,
                sha1(request.v),
            )
        ) {
            sendError(
                request,
                PROTOCOL_ERROR,
                "Invalid Token; tokens expire after " + TOKEN_TIMEOUT + "ms; " +
                    "only valid for the IP/port to which it was issued;" +
                    " only valid for the info hash for which it was issued",
            )
            return
        }

        // Note: right now no data is stored (someday in the future, when server is supported)

        // send a proper response to indicate everything is OK
        val rsp =
            PutResponse(
                address = request.address,
                id = nodeId,
                tid = request.tid,
                ip = request.address,
            )
        sendMessage(rsp)
    }

    internal suspend fun announce(request: AnnounceRequest) {
        // first check if the token is OK
        if (!tokenManager.checkToken(
                request.token,
                request.id,
                request.address.address,
                request.address.port,
                request.infoHash,
            )
        ) {
            sendError(
                request,
                PROTOCOL_ERROR,
                "Invalid Token; tokens expire after " + TOKEN_TIMEOUT + "ms; " +
                    "only valid for the IP/port to which it was issued;" +
                    " only valid for the info hash for which it was issued",
            )
            return
        }

        // everything OK, so store the value

        database.store(request.infoHash, request.address)

        // send a proper response to indicate everything is OK
        val rsp =
            AnnounceResponse(
                address = request.address,
                id = nodeId,
                tid = request.tid,
                ip = request.address,
            )
        sendMessage(rsp)
    }

    internal suspend fun sendError(
        origMsg: Message,
        code: Int,
        msg: String,
    ) {
        sendMessage(
            Error(
                address = origMsg.address,
                id = nodeId,
                tid = origMsg.tid,
                code = code,
                message = msg.encodeToByteArray(),
            ),
        )
    }

    internal fun recieved(
        msg: Response,
        associatedCall: Call?,
    ) {
        val ip = msg.address
        val id = msg.id

        val expectedId = associatedCall?.expectedID

        // server only verifies IP equality for responses.
        // we only want remote nodes with stable ports in our routing table, so appley a stricter check here
        if (associatedCall != null &&
            associatedCall.request.address != associatedCall.response!!.address
        ) {
            return
        }

        val entryById = routingTable.findPeerById(id)

        // entry is claiming the same ID as entry with different IP in our routing table -> ignore
        if (entryById != null && entryById.address != ip) return

        // ID mismatch from call (not the same as ID mismatch from routing table)
        // it's fishy at least. don't insert even if it proves useful during a lookup
        if (entryById == null && expectedId != null && !expectedId.contentEquals(id)) return

        if (entryById != null) {
            return
        }

        val newEntry = Peer(id, msg.address)

        routingTable.insert(newEntry)
    }

    internal suspend fun ping(
        address: Address,
        id: ByteArray?,
    ) {
        val tid = createTid()
        val pr =
            PingRequest(
                address = address,
                id = nodeId,
                tid = tid,
                ro = readOnlyState,
            )
        doRequestCall(pr, id) // expectedId can not be available (only address is known)
    }

    private suspend fun handlePacket(
        reader: BEReader,
        address: Address,
    ) {
        val map: Map<String, BEObject>
        try {
            map = (reader.decodeBencode() as BEMap).toMap()
        } catch (throwable: Throwable) {
            debug(throwable)
            return
        }

        val msg: Message
        try {
            msg = parseMessage(address, map) { tid: Long ->
                requestCalls[tid]?.request
            } ?: return
        } catch (throwable: Throwable) {
            debug(throwable)
            return
        }

        if (msg is Request) {
            // if readOnlyState is true we are in "Read-Only State"
            // It no longer responds to 'query' messages that it receives,
            // that is messages containing a 'q' flag in the top-level dictionary.
            if (!readOnlyState) {
                when (msg) {
                    is PutRequest -> put(msg)
                    is GetRequest -> get(msg)
                    is AnnounceRequest -> announce(msg)
                    is FindNodeRequest -> findNode(msg)
                    is GetPeersRequest -> getPeers(msg)
                    is PingRequest -> ping(msg)
                }
            }

            return
        }

        // check if this is a response to an outstanding request
        val call = requestCalls[msg.tid]

        if (call != null) {
            requestCalls.remove(msg.tid)
            if (call.request.address == msg.address) {
                if (msg is Response) {
                    call.response(msg)
                    recieved(msg, call)
                    call.done()
                }

                if (msg is Error) {
                    timeout(call)
                }

                return
            }
            timeout(call)
        }

        debug("ignoring message " + msg.toString())
    }

    internal suspend fun sendMessage(msg: Message) {
        requireNotNull(msg.address) { "message destination must not be null" }

        send(msg, null)
    }
}

suspend fun newNott(
    nodeId: ByteArray,
    bootstrap: Set<Address> = defaultBootstrap(),
): Nott {
    val nott = Nott(nodeId, bootstrap = bootstrap)
    nott.startup()
    nott.bootstrap()
    return nott
}

fun nodeId(): ByteArray {
    val id = ByteArray(SHA1_HASH_LENGTH)
    id[0] = '-'.code.toByte()
    id[1] = 'N'.code.toByte()
    id[2] = 'O'.code.toByte()
    id[3] = '0'.code.toByte()
    id[4] = '8'.code.toByte()
    id[5] = '1'.code.toByte()
    id[6] = '5'.code.toByte()
    id[7] = '-'.code.toByte()
    return Random.nextBytes(id, 8)
}

fun defaultBootstrap(): Set<Address> {
    val result = mutableSetOf<Address>()

    result.addAll(allByName("dht.transmissionbt.com", 6881))
    result.addAll(allByName("dht.libtorrent.org", 25401))
    // result.addAll(allByName("router.bittorrent.com", 6881)) // not responding

    return result
}

@Suppress("SameParameterValue")
private fun allByName(
    hostname: String,
    port: Int,
): Set<Address> {
    val result = mutableSetOf<Address>()
    try {
        val inets = InetAddress.getAllByName(hostname)
        inets.forEach { inet ->
            result.add(createAddress(inet, port))
        }
    } catch (throwable: Throwable) {
        debug(throwable)
    }
    return result
}

internal fun sha1(bytes: ByteArray): ByteArray {
    val digest = SHA1()
    digest.update(bytes)
    return digest.digest()
}

internal fun createRandomKey(length: Int): ByteArray = Random.nextBytes(ByteArray(length))

internal fun createTid(): Long = createRandomKey(TID_LENGTH).toLong(TID_LENGTH)

@Suppress("SameReturnValue")
private val isError: Boolean
    get() = true

@Suppress("SameReturnValue")
private val isDebug: Boolean
    get() = false

internal fun debug(text: String) {
    if (isDebug) {
        println(text)
    }
}

internal fun debug(throwable: Throwable) {
    if (isError) {
        throwable.printStackTrace()
    }
}
