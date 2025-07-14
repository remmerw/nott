package io.github.remmerw.nott


internal enum class CallState {
    UNSENT,
    SENT,
    STALLED,
    ERROR,
    RESPONDED
}
