package com.pinalarmlock.app.lockwatch

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

data class PermissionSettingsTarget(
    val action: String,
    val dataUri: String? = null,
)

object AppLockSettings {
    fun usageAccess(packageName: String) =
        PermissionSettingsTarget(
            action = Settings.ACTION_USAGE_ACCESS_SETTINGS,
            dataUri = "package:$packageName",
        )

    fun usageAccessList() =
        PermissionSettingsTarget(
            action = Settings.ACTION_USAGE_ACCESS_SETTINGS,
        )

    fun overlay(packageName: String) =
        PermissionSettingsTarget(
            action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            dataUri = "package:$packageName",
        )

    fun toIntent(target: PermissionSettingsTarget): Intent =
        Intent(target.action).apply {
            target.dataUri?.let { data = Uri.parse(it) }
        }

    fun start(
        context: Context,
        target: PermissionSettingsTarget,
        fallback: PermissionSettingsTarget? = null,
    ) {
        try {
            context.startActivity(toIntent(target))
        } catch (_: ActivityNotFoundException) {
            if (fallback != null) {
                context.startActivity(toIntent(fallback))
            }
        }
    }
}
