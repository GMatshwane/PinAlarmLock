package com.pinalarmlock.app.lockwatch

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent

object DeviceLock {
    fun confirmIntent(
        context: Context,
        title: String,
        description: String,
    ): Intent? {
        val keyguard = context.getSystemService(KeyguardManager::class.java) ?: return null
        if (!keyguard.isDeviceSecure) return null
        @Suppress("DEPRECATION")
        return keyguard.createConfirmDeviceCredentialIntent(title, description)
    }
}
