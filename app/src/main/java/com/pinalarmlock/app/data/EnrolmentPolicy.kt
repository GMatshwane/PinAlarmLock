package com.pinalarmlock.app.data

import com.pinalarmlock.app.BuildConfig

object EnrolmentPolicy {
    val OWN_PACKAGE: String = BuildConfig.APPLICATION_ID

    val EXCLUDED: Set<String> =
        setOf(
            OWN_PACKAGE,
            "com.android.settings",
            "com.android.systemui",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.permissioncontroller",
        )

    fun isEnrolable(
        packageName: String,
        homePackages: Set<String>,
    ): Boolean {
        if (packageName in EXCLUDED) return false
        if (packageName in homePackages) return false
        return true
    }

    fun filterEnrolable(
        packageNames: List<String>,
        homePackages: Set<String>,
    ): List<String> = packageNames.filter { isEnrolable(it, homePackages) }
}
