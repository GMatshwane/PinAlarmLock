package com.example.pinalarmlock.lockwatch

object ForegroundEvent {
    const val MOVE_TO_FOREGROUND = 1
    const val ACTIVITY_RESUMED = 23

    fun isForegroundEvent(eventType: Int): Boolean =
        eventType == MOVE_TO_FOREGROUND || eventType == ACTIVITY_RESUMED
}

internal data class UsageEventWindow(val start: Long, val end: Long)

internal class UsageEventCursor(initialTime: Long) {
    var lastEventTime: Long = initialTime
        private set

    fun advanceIfReady(enrolledReady: Boolean, now: Long): UsageEventWindow? {
        if (!enrolledReady) return null
        return UsageEventWindow(start = lastEventTime, end = now).also {
            lastEventTime = now
        }
    }
}
