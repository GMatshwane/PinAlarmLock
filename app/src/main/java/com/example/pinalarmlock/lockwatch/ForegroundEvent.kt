package com.example.pinalarmlock.lockwatch

object ForegroundEvent {
    const val MOVE_TO_FOREGROUND = 1
    const val ACTIVITY_RESUMED = 23

    fun isForegroundEvent(eventType: Int): Boolean =
        eventType == MOVE_TO_FOREGROUND || eventType == ACTIVITY_RESUMED
}
