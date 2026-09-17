package com.example.pinalarmlock.ui.lock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.pinalarmlock.R
import kotlin.math.roundToInt

@Composable
fun PinDots(
    length: Int,
    modifier: Modifier = Modifier,
    maxLength: Int = 6,
) {
    Row(
        modifier = modifier.testTag("pinDots"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(maxLength) { index ->
            val filled = index < length
            Box(
                modifier =
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
            )
        }
    }
}

@Composable
fun PinPad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    submitEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val backspaceLabel = stringResource(R.string.key_backspace)
    val submitLabel = stringResource(R.string.key_submit)
    val rows =
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("⌫", "0", "OK"),
        )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { key ->
                    val enabled = key != "OK" || submitEnabled
                    FilledTonalButton(
                        onClick = {
                            when (key) {
                                "⌫" -> onBackspace()
                                "OK" -> onSubmit()
                                else -> onDigit(key.first())
                            }
                        },
                        enabled = enabled,
                        modifier =
                            Modifier
                                .weight(1f)
                                .aspectRatio(1.6f)
                                .semantics {
                                    contentDescription =
                                        when (key) {
                                            "⌫" -> backspaceLabel
                                            "OK" -> submitLabel
                                            else -> key
                                        }
                                }
                                .testTag("key$key"),
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PinShakeBox(
    nonce: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(nonce) {
        if (nonce == 0) return@LaunchedEffect
        repeat(4) {
            offsetX.animateTo(16f, animationSpec = tween(40))
            offsetX.animateTo(-16f, animationSpec = tween(40))
        }
        offsetX.animateTo(0f, animationSpec = tween(40))
    }
    Box(
        modifier = modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) },
    ) {
        content()
    }
}
