package com.example.pinalarmlock.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pinalarmlock.ui.Dest
import com.example.pinalarmlock.ui.LockViewModel
import com.example.pinalarmlock.ui.lock.LockScreen
import com.example.pinalarmlock.ui.setup.SetupScreen
import com.example.pinalarmlock.ui.unlocked.UnlockedScreen

@Composable
fun AppNavHost(
    viewModel: LockViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            when (state.dest) {
                Dest.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                Dest.SetupEnter, Dest.SetupConfirm -> SetupScreen(
                    state = state,
                    onDigit = viewModel::onDigit,
                    onBackspace = viewModel::onBackspace,
                    onSubmit = viewModel::onSubmit,
                )
                Dest.Locked -> LockScreen(
                    state = state,
                    onDigit = viewModel::onDigit,
                    onBackspace = viewModel::onBackspace,
                    onSubmit = viewModel::onSubmit,
                )
                Dest.Unlocked -> UnlockedScreen(onLockAgain = viewModel::onLockAgain)
            }
        }
    }
}
