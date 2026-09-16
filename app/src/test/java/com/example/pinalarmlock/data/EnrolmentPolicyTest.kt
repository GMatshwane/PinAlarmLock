package com.example.pinalarmlock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnrolmentPolicyTest {
    private val launcher = "com.google.android.apps.nexuslauncher"

    @Test
    fun rejectsOwnPackageSettingsSystemUiAndInstallers() {
        val home = setOf(launcher)
        assertFalse(EnrolmentPolicy.isEnrolable(EnrolmentPolicy.OWN_PACKAGE, home))
        assertFalse(EnrolmentPolicy.isEnrolable("com.android.settings", home))
        assertFalse(EnrolmentPolicy.isEnrolable("com.android.systemui", home))
        assertFalse(EnrolmentPolicy.isEnrolable("com.android.packageinstaller", home))
        assertFalse(EnrolmentPolicy.isEnrolable("com.google.android.packageinstaller", home))
        assertFalse(EnrolmentPolicy.isEnrolable("com.android.permissioncontroller", home))
        assertFalse(EnrolmentPolicy.isEnrolable(launcher, home))
    }

    @Test
    fun allowsOrdinaryLaunchableApp() {
        assertTrue(
            EnrolmentPolicy.isEnrolable("com.whatsapp", setOf(launcher)),
        )
    }

    @Test
    fun filterEnrolableDropsExcludedAndKeepsOrder() {
        val input = listOf(
            EnrolmentPolicy.OWN_PACKAGE,
            "com.whatsapp",
            "com.android.settings",
            "com.google.android.apps.photos",
            launcher,
        )
        assertEquals(
            listOf("com.whatsapp", "com.google.android.apps.photos"),
            EnrolmentPolicy.filterEnrolable(input, setOf(launcher)),
        )
    }
}
