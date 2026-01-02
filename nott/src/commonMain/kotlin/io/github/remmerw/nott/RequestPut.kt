package io.github.remmerw.nott

import io.github.remmerw.buri.BEObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap


@OptIn(ExperimentalCoroutinesApi::class)
fun CoroutineScope.requestPut(
    nott: Nott,
    target: ByteArray,
    v: BEObject,
    cas: Long? = null,
    k: ByteArray? = null,
    salt: ByteArray? = null,
    seq: Long? = null,
    sig: ByteArray? = null,
    intermediateTimeout: () -> Long
): ReceiveChannel<InetSocketAddress> = produce {

    val gated: MutableSet<Int> = sortedSetOf()

    while (true) {

        val closest = ClosestSet(nott, target)
        closest.initialize()

        val inFlight: MutableSet<Call> = ConcurrentHashMap.newKeySet()

        do {
            do {
                ensureActive()

                val peer = closest.nextCandidate()

                if (peer != null) {
                    val tid = createRandomKey(TID_LENGTH)
                    val request = GetPeersRequest(
                        address = peer.address,
                        id = nott.nodeId,
                        tid = tid,
                        ro = nott.readOnlyState,
                        infoHash = target
                    )
                    val call = Call(request, peer.id)
                    closest.requestCall(call, peer)
                    inFlight.add(call)
                }
            } while (peer != null)


            ensureActive()

            val removed: MutableSet<Call> = mutableSetOf()
            inFlight.forEach { call ->
                if (call.state() == CallState.RESPONDED) {

                    removed.add(call)
                    val message = call.response
                    if (message is PutResponse) {
                        send(message.address)
                    } else if (message is GetPeersResponse) {

                        val match = closest.acceptResponse(call)
                        if (match != null) {

                            closest.insert(match)

                            if (message.token != null) {

                                if (gated.add(match.hashCode())) {

                                    val tid = createRandomKey(TID_LENGTH)
                                    val request = PutRequest(
                                        address = match.address,
                                        id = nott.nodeId,
                                        tid = tid,
                                        ro = nott.readOnlyState,
                                        token = message.token,
                                        v = v,
                                        cas = cas,
                                        k = k,
                                        salt = salt,
                                        seq = seq,
                                        sig = sig
                                    )

                                    val call = Call(request, match.id)
                                    inFlight.add(call)
                                    nott.doRequestCall(call)
                                }
                            }
                        }
                    }
                } else {
                    val failure = closest.checkTimeoutOrFailure(call)
                    if (failure) {
                        removed.add(call)
                    }
                }
            }

            inFlight.removeAll(removed)
            ensureActive()
        } while (!inFlight.isEmpty())

        val timeout = intermediateTimeout.invoke()
        if (timeout <= 0) {
            break
        } else {
            debug("Timeout lookup for $timeout [ms]")
            delay(timeout)
        }
    }

}




