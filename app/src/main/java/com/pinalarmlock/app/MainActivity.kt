package com.pinalarmlock.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.pinalarmlock.app.alarm.AlarmPlayer
import com.pinalarmlock.app.data.PinRepository
import com.pinalarmlock.app.data.ProtectedAppsRepository
import com.pinalarmlock.app.lockwatch.AppLockPermissions
import com.pinalarmlock.app.lockwatch.AppLockSettings
import com.pinalarmlock.app.lockwatch.DeviceLock
import com.pinalarmlock.app.lockwatch.LaunchableApps
import com.pinalarmlock.app.lockwatch.WatchController
import com.pinalarmlock.app.ui.LockViewModel
import com.pinalarmlock.app.ui.home.HomeViewModel
import com.pinalarmlock.app.ui.navigation.AppNavHost
import com.pinalarmlock.app.ui.theme.PinAlarmLockTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private lateinit var lockViewModel: LockViewModel

    private val deviceLockLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                lockViewModel.onDeviceLockConfirmed()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isGate = intent.getBooleanExtra(EXTRA_GATE, false)
        if (!isGate && Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val app = application as PinAlarmLockApp
        val pinRepository = PinRepository(applicationContext)
        if (isGate) {
            lifecycleScope.launch {
                if (!pinRepository.hasPin()) finish()
            }
        }
        val protectedApps = ProtectedAppsRepository(applicationContext)
        val alarmPlayer = AlarmPlayer(applicationContext)
        lockViewModel =
            ViewModelProvider(
                this,
                LockViewModel.factory(
                    pinRepository = pinRepository,
                    alarmPlayer = alarmPlayer,
                    session = app.lockSession,
                    isGate = isGate,
                    onGateUnlocked = {},
                ),
            )[LockViewModel::class.java]
        lockViewModel.bootstrap()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                lockViewModel.gateUnlockedEvents.collect {
                    finish()
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                lockViewModel.confirmDeviceLockEvents.collect {
                    promptDeviceLock()
                }
            }
        }

        val appContext = applicationContext
        val appPackageManager = appContext.packageManager
        val homeViewModel =
            ViewModelProvider(
                this,
                HomeViewModel.factory(
                    protectedApps = protectedApps,
                    loadApps = { LaunchableApps.load(appPackageManager) },
                    hasUsageAccess = { AppLockPermissions.hasUsageAccess(appContext) },
                    hasOverlay = { AppLockPermissions.hasOverlay(appContext) },
                    onEnrolmentChanged = { WatchController.syncAsync(appContext) },
                ),
            )[HomeViewModel::class.java]

        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START && !isGate && !app.lockSession.isUnlocked) {
                    lockViewModel.bootstrap()
                }
                if (event == Lifecycle.Event.ON_STOP && !isChangingConfigurations) {
                    lockViewModel.onAppBackgrounded()
                }
                if (event == Lifecycle.Event.ON_RESUME) {
                    homeViewModel.refresh()
                    WatchController.syncAsync(this)
                }
            },
        )

        setContent {
            PinAlarmLockTheme {
                val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
                AppNavHost(
                    viewModel = lockViewModel,
                    homeState = homeState,
                    onOpenUsageAccess = {
                        AppLockSettings.start(
                            this,
                            AppLockSettings.usageAccess(packageName),
                            AppLockSettings.usageAccessList(),
                        )
                    },
                    onOpenOverlay = {
                        AppLockSettings.start(this, AppLockSettings.overlay(packageName))
                    },
                    onToggle = homeViewModel::setEnrolled,
                )
            }
        }
    }

    private fun promptDeviceLock() {
        val intent =
            DeviceLock.confirmIntent(
                this,
                getString(R.string.forgot_pin_device_title),
                getString(R.string.forgot_pin_device_subtitle),
            )
        if (intent == null) {
            lockViewModel.onDeviceLockUnavailable()
        } else {
            deviceLockLauncher.launch(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_GATE, false) &&
            (application as PinAlarmLockApp).lockSession.isUnlocked
        ) {
            finish()
        }
    }

    companion object {
        const val EXTRA_GATE = "extra_gate"
    }
}
