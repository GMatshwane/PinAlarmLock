package com.pinalarmlock.app.lockwatch

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.pinalarmlock.app.data.PinRepository
import com.pinalarmlock.app.data.ProtectedAppsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object WatchController {
    private const val TAG = "WatchController"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun syncAsync(context: Context) {
        val appContext = context.applicationContext
        scope.launch { sync(appContext) }
    }

    suspend fun sync(context: Context) {
        val appContext = context.applicationContext
        val hasPin = PinRepository(appContext).hasPin()
        val enrolled = ProtectedAppsRepository(appContext).list().size
        val run =
            WatchEligibility.shouldRunWatcher(
                hasPin = hasPin,
                enrolledCount = enrolled,
                hasUsageAccess = AppLockPermissions.hasUsageAccess(appContext),
                hasOverlay = AppLockPermissions.hasOverlay(appContext),
            )
        val intent = Intent(appContext, LockWatchService::class.java)
        try {
            if (run) {
                if (Build.VERSION.SDK_INT >= 26) {
                    appContext.startForegroundService(intent)
                } else {
                    appContext.startService(intent)
                }
            } else {
                appContext.stopService(intent)
            }
        } catch (e: IllegalStateException) {
            // Covers ForegroundServiceStartNotAllowedException: the app lost its window to start
            // the watcher, so the next foreground sync has to retry instead of crashing here.
            Log.w(TAG, "Could not ${if (run) "start" else "stop"} the lock watcher", e)
        }
    }
}
