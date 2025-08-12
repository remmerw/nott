package io.github.remmerw.nott

import io.github.remmerw.buri.BEObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive


@Suppress("ArrayInDataClass")
data class Data(val v: BEObject, val seq: Long, val k: ByteArray, val sig: ByteArray)

@OptIn(ExperimentalCoroutinesApi::class)
fun CoroutineScope.requestGet(
    nott: Nott,
    key: ByteArray,
    seq: Long? = null,
    intermediateTimeout: () -> Long
): ReceiveChannel<Data> = produce {

    val gated: MutableSet<Int> = mutableSetOf()

    while (true) {

        val closest = ClosestSet(nott, key)
        closest.initialize()

        val inFlight: MutableSet<Call> = mutableSetOf()
        do {
            do {
                ensureActive()

                val peer = closest.nextCandidate(inFlight)

                if (peer != null) {
                    val tid = createRandomKey(TID_LENGTH)

                    val request = GetRequest(
                        address = peer.address,
                        id = nott.nodeId,
                        tid = tid,
                        ro = nott.readOnlyState,
                        target = key,
                        seq = seq
                    )

                    val call = Call(request, peer.id)
                    closest.requestCall(call, peer)
                    inFlight.add(call)
                }
            } while (peer != null)


            ensureActive()

            val removed: MutableList<Call> = mutableListOf()
            inFlight.forEach { call ->
                if (call.state() == CallState.RESPONDED) {
                    removed.add(call)

                    val match = closest.acceptResponse(call)
                    if (match != null) {
                        val rsp = call.response as GetResponse

                        if(rsp.v != null && rsp.seq != null
                            && rsp.k != null && rsp.sig != null) {
                            if (gated.add(match.hashCode())) {
                                val data = Data(
                                    v = rsp.v,
                                    seq = rsp.seq,
                                    k = rsp.k,
                                    sig = rsp.sig
                                )
                                send(data)
                            }
                        }
                        closest.insert(match)
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





