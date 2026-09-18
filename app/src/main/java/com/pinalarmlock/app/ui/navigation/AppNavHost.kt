package com.pinalarmlock.app.ui.navigation

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
import com.pinalarmlock.app.ui.Dest
import com.pinalarmlock.app.ui.LockViewModel
import com.pinalarmlock.app.ui.home.HomeScreen
import com.pinalarmlock.app.ui.home.HomeUiState
import com.pinalarmlock.app.ui.lock.LockScreen
import com.pinalarmlock.app.ui.setup.SetupScreen

@Composable
fun AppNavHost(
    viewModel: LockViewModel,
    homeState: HomeUiState = HomeUiState(),
    onOpenUsageAccess: () -> Unit = {},
    onOpenOverlay: () -> Unit = {},
    onToggle: (String, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
        ) {
            when (state.dest) {
                Dest.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                Dest.SetupEnter, Dest.SetupConfirm, Dest.ChangeCurrent ->
                    SetupScreen(
                        state = state,
                        onDigit = viewModel::onDigit,
                        onBackspace = viewModel::onBackspace,
                        onSubmit = viewModel::onSubmit,
                        onCancel = viewModel::onCancelReset,
                    )
                Dest.Locked ->
                    LockScreen(
                        state = state,
                        onDigit = viewModel::onDigit,
                        onBackspace = viewModel::onBackspace,
                        onSubmit = viewModel::onSubmit,
                        onForgotPin = viewModel::onForgotPin,
                    )
                Dest.Unlocked ->
                    HomeScreen(
                        state = homeState,
                        onOpenUsageAccess = onOpenUsageAccess,
                        onOpenOverlay = onOpenOverlay,
                        onToggle = onToggle,
                        onChangePin = viewModel::startChangePin,
                    )
            }
        }
    }
}
