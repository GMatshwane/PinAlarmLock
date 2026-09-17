package com.pinalarmlock.app.lockwatch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchEligibilityTest {
    @Test
    fun runsOnlyWhenPinEnrolledAndBothPermissions() {
        assertTrue(WatchEligibility.shouldRunWatcher(true, 1, true, true))
        assertFalse(WatchEligibility.shouldRunWatcher(false, 1, true, true))
        assertFalse(WatchEligibility.shouldRunWatcher(true, 0, true, true))
        assertFalse(WatchEligibility.shouldRunWatcher(true, 1, false, true))
        assertFalse(WatchEligibility.shouldRunWatcher(true, 1, true, false))
    }
}
