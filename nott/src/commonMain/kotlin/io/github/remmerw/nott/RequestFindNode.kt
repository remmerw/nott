package io.github.remmerw.nott

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.net.InetSocketAddress


/**
 * Returns all nodes on the way
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun CoroutineScope.findNode(
    nott: Nott,
    target: ByteArray,
    intermediateTimeout: () -> Long
): ReceiveChannel<InetSocketAddress> = produce {

    val gated: MutableSet<Int> = sortedSetOf()

    while (true) {

        val closest = ClosestSet(nott, target)
        closest.initialize()

        val inFlight: MutableSet<Call> = mutableSetOf()

        do {
            do {
                ensureActive()

                val peer = closest.nextCandidate()

                if (peer != null) {
                    val tid = createRandomKey(TID_LENGTH)
                    val request = FindNodeRequest(
                        address = peer.address,
                        id = nott.nodeId,
                        tid = tid,
                        ro = nott.readOnlyState,
                        target = target
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

                    val message = call.response
                    message as FindNodeResponse
                    val match = closest.acceptResponse(call)

                    if (match != null) {
                        if (target.contentEquals(match.id)) {

                            if (gated.add(match.hashCode())) {
                                send(match.address)
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






