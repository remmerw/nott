package io.github.remmerw.nott

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
fun CoroutineScope.requestAnnounce(
    nott: Nott,
    target: ByteArray,
    port: Int,
    intermediateTimeout: () -> Long,
): ReceiveChannel<InetSocketAddress> =
    produce {
        val gated: MutableSet<Long> = sortedSetOf()

        while (true) {
            val closest = ClosestSet(nott, target)
            closest.initialize()

            val inFlight: MutableSet<Call> = ConcurrentHashMap.newKeySet()

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

                        val rsp = call.response
                        if (rsp is AnnounceResponse) {
                            send(rsp.address)
                        }
                        if (rsp is GetPeersResponse) {
                            val match = closest.acceptResponse(call)

                            if (match != null) {
                                if (rsp.token != null) {
                                    closest.insert(match)

                                    if (gated.add(match.id.toLongKey())) {
                                        val tid = createTid()
                                        val request =
                                            AnnounceRequest(
                                                address = match.address,
                                                id = nott.nodeId,
                                                tid = tid,
                                                ro = nott.readOnlyState,
                                                infoHash = target,
                                                port = port,
                                                token = rsp.token,
                                                name = null,
                                            )

                                        val call = nott.doRequestCall(request, match.id)
                                        inFlight.add(call)
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
                delay(timeout.milliseconds)
            }
        }
    }
