package io.github.remmerw.nott

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Tests {
    @Test
    fun testId() {
        val nodeId = nodeId()
        assertEquals(nodeId.size, 20)

        val name = nodeId.decodeToString()
        println(name)
        assertTrue(name.startsWith("-NO0815-"))
    }
}