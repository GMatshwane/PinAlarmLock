package com.example.pinalarmlock.lockwatch

object WatchEligibility {
    fun shouldRunWatcher(
        hasPin: Boolean,
        enrolledCount: Int,
        hasUsageAccess: Boolean,
        hasOverlay: Boolean,
    ): Boolean = hasPin && enrolledCount > 0 && hasUsageAccess && hasOverlay
}
