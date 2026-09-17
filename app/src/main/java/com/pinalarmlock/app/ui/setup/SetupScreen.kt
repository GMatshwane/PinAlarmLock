package com.pinalarmlock.app.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pinalarmlock.app.R
import com.pinalarmlock.app.ui.Dest
import com.pinalarmlock.app.ui.LockUiState
import com.pinalarmlock.app.ui.lock.PinDots
import com.pinalarmlock.app.ui.lock.PinPad
import com.pinalarmlock.app.ui.lock.PinShakeBox

@Composable
fun SetupScreen(
    state: LockUiState,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConfirm = state.dest == Dest.SetupConfirm
    PinShakeBox(nonce = state.shakeNonce, modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text =
                        stringResource(
                            if (isConfirm) R.string.setup_confirm_title else R.string.setup_enter_title,
                        ),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text =
                        stringResource(
                            if (isConfirm) R.string.setup_confirm_subtitle else R.string.setup_enter_subtitle,
                        ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(32.dp))
                PinDots(length = state.enteredPin.length)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = state.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    minLines = 1,
                )
            }
            PinPad(
                onDigit = onDigit,
                onBackspace = onBackspace,
                onSubmit = onSubmit,
                submitEnabled = state.enteredPin.length in 4..6,
            )
        }
    }
}
