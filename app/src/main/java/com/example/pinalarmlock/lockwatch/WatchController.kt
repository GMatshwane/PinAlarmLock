package com.example.pinalarmlock.lockwatch

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.pinalarmlock.data.PinRepository
import com.example.pinalarmlock.data.ProtectedAppsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object WatchController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun syncAsync(context: Context) {
        val appContext = context.applicationContext
        scope.launch { sync(appContext) }
    }

    suspend fun sync(context: Context) {
        val appContext = context.applicationContext
        val hasPin = PinRepository(appContext).hasPin()
        val enrolled = ProtectedAppsRepository(appContext).list().size
        val run = WatchEligibility.shouldRunWatcher(
            hasPin = hasPin,
            enrolledCount = enrolled,
            hasUsageAccess = AppLockPermissions.hasUsageAccess(appContext),
            hasOverlay = AppLockPermissions.hasOverlay(appContext),
        )
        val intent = Intent(appContext, LockWatchService::class.java)
        if (run) {
            if (Build.VERSION.SDK_INT >= 26) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
        } else {
            appContext.stopService(intent)
        }
    }
}
