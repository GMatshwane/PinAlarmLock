package com.example.pinalarmlock.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmDurationTest {
    @Test
    fun alarmDurationIsTenSeconds() {
        assertEquals(10_000L, AlarmDuration.ALARM_DURATION_MS)
    }
}
