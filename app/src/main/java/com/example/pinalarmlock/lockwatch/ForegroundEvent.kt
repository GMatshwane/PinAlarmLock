package com.example.pinalarmlock.lockwatch

import android.app.usage.UsageEvents

object ForegroundEvent {
    /** Same value as the deprecated `MOVE_TO_FOREGROUND`; `ACTIVITY_STOPPED` is 23, not this. */
    const val ACTIVITY_RESUMED = UsageEvents.Event.ACTIVITY_RESUMED

    fun isForegroundEvent(eventType: Int): Boolean = eventType == ACTIVITY_RESUMED
}

internal data class UsageEventWindow(val start: Long, val end: Long)

internal data class ObservedEvent(val timestamp: Long, val packageName: String)

internal class UsageEventCursor(
    initialTime: Long,
    private val lookBackMs: Long = LOOK_BACK_MS,
) {
    var lastEventTime: Long = initialTime
        private set

    private val handled = mutableSetOf<ObservedEvent>()

    fun windowIfReady(
        enrolledReady: Boolean,
        now: Long,
    ): UsageEventWindow? {
        if (!enrolledReady) return null
        return UsageEventWindow(start = maxOf(lastEventTime, now - lookBackMs), end = now)
    }

    /**
     * Drops events already handled and advances the cursor to the newest observed event instead of
     * to the query end, so events the system reports late are still delivered by a later window.
     */
    fun accept(events: List<ObservedEvent>): List<ObservedEvent> {
        val fresh = events.filterNot { it in handled }
        if (fresh.isEmpty()) return emptyList()
        lastEventTime = maxOf(lastEventTime, fresh.maxOf { it.timestamp })
        handled += fresh
        handled.removeAll { it.timestamp < lastEventTime }
        return fresh
    }

    companion object {
        const val LOOK_BACK_MS = 10_000L
    }
}
