package com.pinalarmlock.app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.pinalarmlock.app.data.EnrolmentPolicy
import com.pinalarmlock.app.session.LockSession

class PinAlarmLockApp : Application() {
    val lockSession = LockSession(EnrolmentPolicy.OWN_PACKAGE)

    /** Owned here, not by the watcher, so the session still re-locks while no service runs. */
    private val screenOff =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                lockSession.lock()
            }
        }

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this,
            screenOff,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }
}
