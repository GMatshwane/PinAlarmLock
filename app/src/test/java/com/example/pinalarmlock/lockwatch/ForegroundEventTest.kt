package com.example.pinalarmlock.lockwatch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundEventTest {
    @Test
    fun acceptsResumeAndMoveToForegroundOnly() {
        assertTrue(ForegroundEvent.isForegroundEvent(1))
        assertTrue(ForegroundEvent.isForegroundEvent(23))
        assertFalse(ForegroundEvent.isForegroundEvent(2))
        assertFalse(ForegroundEvent.isForegroundEvent(24))
    }
}
