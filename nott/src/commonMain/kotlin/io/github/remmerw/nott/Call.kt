package io.github.remmerw.nott

import kotlin.concurrent.Volatile
import kotlin.time.TimeSource
import java.net.InetSocketAddress
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

internal class Call(
    val tid: ByteArray,
    val address: InetSocketAddress,
    val expectedID: ByteArray?,
) {
    var sentTime: ValueTimeMark? = null
        private set

    @Volatile
    private var state: CallState = CallState.UNSENT

    var response: Message? = null
        private set

    fun matchesExpectedID(): Boolean = expectedID!!.contentEquals(response!!.id)

    fun injectError() {
        state = CallState.ERROR
    }

    fun response(rsp: Message) {
        response = rsp

        state =
            when (rsp) {
                is Response -> CallState.RESPONDED
                is Error -> CallState.ERROR
                else -> throw IllegalStateException("should not happen")
            }
    }

    fun hasSend() {
        sentTime = TimeSource.Monotonic.markNow()

        state = CallState.SENT
    }

    fun state(): CallState = state
}
