package com.example.pinalarmlock

import android.app.Application
import com.example.pinalarmlock.data.EnrolmentPolicy
import com.example.pinalarmlock.session.LockSession

class PinAlarmLockApp : Application() {
    val lockSession = LockSession(EnrolmentPolicy.OWN_PACKAGE)
}
