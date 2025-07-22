package io.github.remmerw.nott

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.net.InetSocketAddress


@OptIn(ExperimentalCoroutinesApi::class)
fun CoroutineScope.requestGetPeers(
    nott: Nott,
    target: ByteArray,
    intermediateTimeout: () -> Long
): ReceiveChannel<InetSocketAddress> = produce {


    val peers: MutableSet<Address> = mutableSetOf()


    while (true) {

        val closest = ClosestSet(nott, target)

        val inFlight: MutableSet<Call> = mutableSetOf()

        do {
            do {
                ensureActive()

                val peer = closest.nextCandidate(inFlight)
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

            val removed: MutableList<Call> = mutableListOf()
            inFlight.forEach { call ->
                if (call.state() == CallState.RESPONDED) {
                    removed.add(call)

                    val match = closest.acceptResponse(call)

                    if (match != null) {
                        val message = call.response
                        message as GetPeersResponse

                        for (item in message.values) {
                            if (peers.add(item)) {
                                try {
                                    send(item.toInetSocketAddress())
                                } catch (throwable: Throwable) {
                                    debug(throwable)
                                }
                            }
                        }

                        // if we scrape we don't care about tokens.
                        // otherwise we're only done if we have found the closest
                        // nodes that also returned tokens
                        if (message.token != null) {
                            closest.insert(match)
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






