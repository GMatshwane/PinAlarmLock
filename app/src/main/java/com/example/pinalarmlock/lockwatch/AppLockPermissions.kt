package com.example.pinalarmlock.lockwatch

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.provider.Settings

object AppLockPermissions {
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode =
            if (Build.VERSION.SDK_INT >= 29) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlay(context: Context): Boolean = Settings.canDrawOverlays(context)
}
