package com.example.pinalarmlock.lockwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchableAppsTest {
    @Test
    fun sortsByLabelThenPackage() {
        val apps = listOf(
            LaunchableApp("b.pkg", "Zebra"),
            LaunchableApp("a.pkg", "Apple"),
            LaunchableApp("c.pkg", "Apple"),
        )
        assertEquals(
            listOf("a.pkg", "c.pkg", "b.pkg"),
            LaunchableApps.sorted(apps).map { it.packageName },
        )
    }
}
