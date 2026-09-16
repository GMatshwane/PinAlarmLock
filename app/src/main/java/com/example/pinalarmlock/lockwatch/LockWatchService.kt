package com.example.pinalarmlock.lockwatch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.pinalarmlock.MainActivity
import com.example.pinalarmlock.PinAlarmLockApp
import com.example.pinalarmlock.R
import com.example.pinalarmlock.data.ProtectedAppsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LockWatchService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val eventCursor = UsageEventCursor(System.currentTimeMillis())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    @Volatile
    private var enrolled: Set<String> = emptySet()

    @Volatile
    private var enrolledReady = false

    private val poll = object : Runnable {
        override fun run() {
            refreshEnrolled()
            pollOnce()
            handler.postDelayed(this, POLL_MS)
        }
    }
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            (application as PinAlarmLockApp).lockSession.lock()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.watch_notification_title),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.watch_notification_title))
            .setContentText(getString(R.string.watch_notification_text))
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        if (!started) {
            registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
            started = true
            handler.post(poll)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        if (started) {
            unregisterReceiver(screenOff)
        }
        started = false
        super.onDestroy()
    }

    private fun refreshEnrolled() {
        scope.launch {
            enrolled = ProtectedAppsRepository(applicationContext).list()
            enrolledReady = true
        }
    }

    private fun pollOnce() {
        if (!enrolledReady) return
        val usm = getSystemService(UsageStatsManager::class.java) ?: return
        val now = System.currentTimeMillis()
        val window = eventCursor.advanceIfReady(enrolledReady, now) ?: return
        val events = usm.queryEvents(window.start, window.end)
        val event = UsageEvents.Event()
        var lastPkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (ForegroundEvent.isForegroundEvent(event.eventType)) {
                lastPkg = event.packageName
            }
        }
        val packageName = lastPkg ?: return
        maybeGate(packageName)
    }

    private fun maybeGate(packageName: String) {
        val session = (application as PinAlarmLockApp).lockSession
        if (!session.shouldGate(packageName, enrolled)) return
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            putExtra(MainActivity.EXTRA_GATE, true)
        }
        startActivity(intent)
    }

    companion object {
        const val CHANNEL_ID = "app_lock"
        const val NOTIFICATION_ID = 42
        const val POLL_MS = 250L
    }
}
