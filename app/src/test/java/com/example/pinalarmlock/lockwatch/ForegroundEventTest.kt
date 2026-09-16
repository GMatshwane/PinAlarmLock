package com.example.pinalarmlock.lockwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun eventCursorDoesNotAdvanceUntilEnrolledAppsAreReady() {
        val cursor = UsageEventCursor(initialTime = 100L)

        assertNull(cursor.advanceIfReady(enrolledReady = false, now = 200L))
        assertEquals(100L, cursor.lastEventTime)
        assertEquals(
            UsageEventWindow(start = 100L, end = 300L),
            cursor.advanceIfReady(enrolledReady = true, now = 300L),
        )
        assertEquals(300L, cursor.lastEventTime)
    }
}
