package com.pinalarmlock.app.lockwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLockSettingsTest {
    @Test
    fun usageAccessOpensThisPackageNotTheFullList() {
        val target = AppLockSettings.usageAccess("com.pinalarmlock.app")

        assertEquals("android.settings.USAGE_ACCESS_SETTINGS", target.action)
        assertEquals("package:com.pinalarmlock.app", target.dataUri)
    }

    @Test
    fun usageAccessFallsBackToTheListWhenThePackagePageIsMissing() {
        val fallback = AppLockSettings.usageAccessList()

        assertEquals("android.settings.USAGE_ACCESS_SETTINGS", fallback.action)
        assertNull(fallback.dataUri)
    }

    @Test
    fun overlayKeepsPackageUriForPreAndroid11Devices() {
        val target = AppLockSettings.overlay("com.pinalarmlock.app")

        assertEquals("android.settings.action.MANAGE_OVERLAY_PERMISSION", target.action)
        assertEquals("package:com.pinalarmlock.app", target.dataUri)
    }
}
