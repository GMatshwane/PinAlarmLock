package com.example.pinalarmlock.lockwatch

import android.app.usage.UsageEvents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundEventTest {
    @Suppress("DEPRECATION")
    @Test
    fun acceptsActivityResumedOnly() {
        assertTrue(ForegroundEvent.isForegroundEvent(UsageEvents.Event.ACTIVITY_RESUMED))
        assertTrue(ForegroundEvent.isForegroundEvent(UsageEvents.Event.MOVE_TO_FOREGROUND))
        assertFalse(ForegroundEvent.isForegroundEvent(UsageEvents.Event.ACTIVITY_STOPPED))
        assertFalse(ForegroundEvent.isForegroundEvent(UsageEvents.Event.ACTIVITY_PAUSED))
        assertFalse(ForegroundEvent.isForegroundEvent(UsageEvents.Event.MOVE_TO_BACKGROUND))
    }

    @Test
    fun eventCursorGivesNoWindowUntilEnrolledAppsAreReady() {
        val cursor = UsageEventCursor(initialTime = 100L)

        assertNull(cursor.windowIfReady(enrolledReady = false, now = 200L))
        assertEquals(
            UsageEventWindow(start = 100L, end = 300L),
            cursor.windowIfReady(enrolledReady = true, now = 300L),
        )
    }

    @Test
    fun eventCursorAdvancesToNewestEventNotToQueryEnd() {
        val cursor = UsageEventCursor(initialTime = 100L)

        cursor.windowIfReady(enrolledReady = true, now = 5_000L)
        cursor.accept(listOf(ObservedEvent(200L, "com.whatsapp")))

        assertEquals(200L, cursor.lastEventTime)
        assertEquals(
            UsageEventWindow(start = 200L, end = 5_000L),
            cursor.windowIfReady(enrolledReady = true, now = 5_000L),
        )
    }

    @Test
    fun eventCursorWindowIsBoundedByLookBack() {
        val cursor = UsageEventCursor(initialTime = 0L, lookBackMs = 10_000L)

        assertEquals(
            UsageEventWindow(start = 90_000L, end = 100_000L),
            cursor.windowIfReady(enrolledReady = true, now = 100_000L),
        )
    }

    @Test
    fun eventCursorDedupesRepeatedEventsFromOverlappingWindows() {
        val cursor = UsageEventCursor(initialTime = 100L)
        val resumed = ObservedEvent(200L, "com.whatsapp")

        assertEquals(listOf(resumed), cursor.accept(listOf(resumed)))
        assertEquals(emptyList<ObservedEvent>(), cursor.accept(listOf(resumed)))
    }

    @Test
    fun eventCursorKeepsLateEventsInsideTheOverlap() {
        val cursor = UsageEventCursor(initialTime = 100L)
        val resumed = ObservedEvent(300L, "com.whatsapp")
        cursor.accept(listOf(resumed))

        val late = ObservedEvent(300L, "com.google.android.apps.photos")

        assertEquals(listOf(late), cursor.accept(listOf(resumed, late)))
        assertEquals(300L, cursor.lastEventTime)
    }
}
