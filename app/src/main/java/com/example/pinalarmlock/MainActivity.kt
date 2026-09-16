package com.example.pinalarmlock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import com.example.pinalarmlock.alarm.AlarmPlayer
import com.example.pinalarmlock.data.PinRepository
import com.example.pinalarmlock.data.ProtectedAppsRepository
import com.example.pinalarmlock.lockwatch.AppLockPermissions
import com.example.pinalarmlock.lockwatch.LaunchableApps
import com.example.pinalarmlock.lockwatch.WatchController
import com.example.pinalarmlock.ui.LockViewModel
import com.example.pinalarmlock.ui.home.HomeViewModel
import com.example.pinalarmlock.ui.navigation.AppNavHost
import com.example.pinalarmlock.ui.theme.PinAlarmLockTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

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
        val lockViewModel = ViewModelProvider(
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

        val appContext = applicationContext
        val appPackageManager = appContext.packageManager
        val homeViewModel = ViewModelProvider(
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
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    onOpenOverlay = {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName"),
                            ),
                        )
                    },
                    onToggle = homeViewModel::setEnrolled,
                )
            }
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
