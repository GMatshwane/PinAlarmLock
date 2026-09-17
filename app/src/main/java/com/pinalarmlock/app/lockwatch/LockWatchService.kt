package com.pinalarmlock.app.lockwatch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pinalarmlock.app.MainActivity
import com.pinalarmlock.app.PinAlarmLockApp
import com.pinalarmlock.app.R
import com.pinalarmlock.app.data.ProtectedAppsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LockWatchService : Service() {
    private var pollThread: HandlerThread? = null
    private var pollHandler: Handler? = null
    private val eventCursor = UsageEventCursor(System.currentTimeMillis())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    @Volatile
    private var enrolled: Set<String> = emptySet()

    @Volatile
    private var enrolledReady = false

    private val poll =
        object : Runnable {
            override fun run() {
                pollOnce()
                pollHandler?.postDelayed(this, POLL_MS)
            }
        }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        observeEnrolled()
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

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val pending =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification: Notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
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
            started = true
            val thread = HandlerThread(POLL_THREAD_NAME).also { it.start() }
            pollThread = thread
            pollHandler = Handler(thread.looper).also { it.post(poll) }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        pollHandler?.removeCallbacks(poll)
        pollHandler = null
        pollThread?.quitSafely()
        pollThread = null
        started = false
        scope.cancel()
        super.onDestroy()
    }

    private fun observeEnrolled() {
        val repository = ProtectedAppsRepository(applicationContext)
        scope.launch {
            repository.packages.collect { packages ->
                enrolled = packages
                enrolledReady = true
            }
        }
    }

    private fun pollOnce() {
        val usm = getSystemService(UsageStatsManager::class.java) ?: return
        val now = System.currentTimeMillis()
        val window = eventCursor.windowIfReady(enrolledReady, now) ?: return
        val events = usm.queryEvents(window.start, window.end)
        val event = UsageEvents.Event()
        val observed = mutableListOf<ObservedEvent>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (ForegroundEvent.isForegroundEvent(event.eventType)) {
                observed += ObservedEvent(event.timeStamp, event.packageName)
            }
        }
        val packageName = eventCursor.accept(observed).lastOrNull()?.packageName ?: return
        maybeGate(packageName)
    }

    private fun maybeGate(packageName: String) {
        val session = (application as PinAlarmLockApp).lockSession
        if (!session.shouldGate(packageName, enrolled)) return
        val intent =
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                putExtra(MainActivity.EXTRA_GATE, true)
            }
        startActivity(intent)
    }

    companion object {
        const val CHANNEL_ID = "app_lock"
        const val NOTIFICATION_ID = 42
        const val POLL_MS = 250L
        const val POLL_THREAD_NAME = "lock-watch-poll"
    }
}
