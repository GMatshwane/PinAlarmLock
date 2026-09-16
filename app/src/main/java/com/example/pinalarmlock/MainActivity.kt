package com.example.pinalarmlock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import com.example.pinalarmlock.alarm.AlarmPlayer
import com.example.pinalarmlock.data.PinRepository
import com.example.pinalarmlock.ui.LockViewModel
import com.example.pinalarmlock.ui.navigation.AppNavHost
import com.example.pinalarmlock.ui.theme.PinAlarmLockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val pinRepository = PinRepository(applicationContext)
        val alarmPlayer = AlarmPlayer(applicationContext)
        val viewModel = ViewModelProvider(
            this,
            LockViewModel.factory(pinRepository, alarmPlayer),
        )[LockViewModel::class.java]
        viewModel.bootstrap()

        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP && !isChangingConfigurations) {
                    viewModel.onAppBackgrounded()
                }
            },
        )

        setContent {
            PinAlarmLockTheme {
                AppNavHost(viewModel = viewModel)
            }
        }
    }

    companion object {
        const val EXTRA_GATE = "extra_gate"
    }
}
