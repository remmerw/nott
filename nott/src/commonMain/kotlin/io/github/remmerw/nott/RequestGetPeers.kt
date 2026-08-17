package io.github.remmerw.nott

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.time.Duration.Companion.milliseconds

data class PeerResponse(
    val peer: Address,
    val addresses: List<Address>,
)

@OptIn(ExperimentalCoroutinesApi::class)
fun CoroutineScope.requestGetPeers(
    nott: Nott,
    target: ByteArray,
    intermediateTimeout: () -> Long,
): ReceiveChannel<PeerResponse> =
    produce {
        val gated: MutableSet<Int> = sortedSetOf()

        while (true) {
            val closest = ClosestSet(nott, target)
            closest.initialize()

            val inFlight: MutableList<Call> = mutableListOf()

            do {
                do {
                    ensureActive()

                    val peer = closest.nextCandidate()
                    if (peer != null) {
                        val tid = createTid()
                        val request =
                            GetPeersRequest(
                                address = peer.address,
                                id = nott.nodeId,
                                tid = tid,
                                ro = nott.readOnlyState,
                                infoHash = target,
                            )
                        val call = closest.requestCall(request, peer)

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

                            val list = mutableListOf<Address>()
                            for (item in message.values) {
                                if (gated.add(item.hashCode())) {
                                    list.add(item)
                                }
                            }

                            if (list.isNotEmpty()) {
                                send(PeerResponse(message.address, list))
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
                delay(timeout.milliseconds)
            }
        }
    }
