package com.pinalarmlock.app.lockwatch

import android.content.Intent
import android.content.pm.PackageManager
import com.pinalarmlock.app.data.EnrolmentPolicy

data class LaunchableApp(
    val packageName: String,
    val label: String,
)

object LaunchableApps {
    fun sorted(apps: List<LaunchableApp>): List<LaunchableApp> =
        apps.sortedWith(
            compareBy<LaunchableApp, String>(String.CASE_INSENSITIVE_ORDER) { it.label }
                .thenBy { it.packageName },
        )

    fun homePackages(pm: PackageManager): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return pm.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }.toSet()
    }

    fun load(pm: PackageManager): List<LaunchableApp> {
        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val homes = homePackages(pm)
        val raw =
            pm.queryIntentActivities(launch, 0).map { ri ->
                LaunchableApp(
                    packageName = ri.activityInfo.packageName,
                    label = ri.loadLabel(pm).toString(),
                )
            }
        val enrolable = EnrolmentPolicy.filterEnrolable(raw.map { it.packageName }, homes).toSet()
        return sorted(raw.filter { it.packageName in enrolable }.distinctBy { it.packageName })
    }
}
