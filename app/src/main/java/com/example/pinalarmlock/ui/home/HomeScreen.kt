package com.example.pinalarmlock.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pinalarmlock.R

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenUsageAccess: () -> Unit,
    onOpenOverlay: () -> Unit,
    onToggle: (packageName: String, enrolled: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        if (!state.canEnrol) {
            Text(
                text = stringResource(R.string.permissions_needed),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (!state.usageGranted) {
            Button(onClick = onOpenUsageAccess, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.permission_usage))
            }
        }
        if (!state.overlayGranted) {
            Button(onClick = onOpenOverlay, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.permission_overlay))
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(state.apps, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(app.label, modifier = Modifier.weight(1f))
                    Switch(
                        checked = app.enrolled,
                        onCheckedChange = { onToggle(app.packageName, it) },
                        enabled = state.canEnrol,
                    )
                }
            }
        }
    }
}
