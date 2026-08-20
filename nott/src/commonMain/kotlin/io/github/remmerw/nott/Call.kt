package io.github.remmerw.nott

import kotlin.concurrent.Volatile
import kotlin.time.TimeSource
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

internal class Call(
    val request: Request,
    val expectedID: ByteArray?,
) {
    var sentTime: ValueTimeMark? = null
        private set

    @Volatile
    private var state: CallState = CallState.UNSENT

    var response: Response? = null
        private set

    fun matchesExpectedID(): Boolean = expectedID!!.contentEquals(response!!.id)

    fun injectError() {
        state = CallState.ERROR
    }

    fun response(rsp: Response) {
        response = rsp
    }

    fun done(){
        state = CallState.RESPONDED
    }

    fun hasSend() {
        sentTime = TimeSource.Monotonic.markNow()

        state = CallState.SENT
    }

    fun state(): CallState = state
}
