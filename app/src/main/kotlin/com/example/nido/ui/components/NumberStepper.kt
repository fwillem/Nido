package com.example.nido.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun NumberStepper(
    value: Int,
    values: List<Int>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    spacing: Dp = 8.dp,
    wrapAround: Boolean = false,
    buttonMinWidth: Dp = 40.dp,
    label: @Composable (Int) -> Unit
) {
    val list = if (values.isEmpty()) listOf(value) else values
    val currentIndex = list.indexOf(value).let { if (it == -1) 0 else it }
    val canDec = wrapAround || currentIndex > 0
    val canInc = wrapAround || currentIndex < list.lastIndex
    val haptic = LocalHapticFeedback.current

    fun bump(delta: Int) {
        if (list.isEmpty()) return
        var newIdx = currentIndex + delta
        newIdx = if (wrapAround) (newIdx % list.size + list.size) % list.size
        else newIdx.coerceIn(0, list.lastIndex)
        if (newIdx != currentIndex) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onValueChange(list[newIdx])
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperButton(
            enabled = canDec,
            onClick = { bump(-1) },
            onLongPressRepeat = { bump(-1) },
            minWidth = buttonMinWidth,
            contentDescription = "Decrease"
        ) {
            Icon(Icons.Filled.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }

        Spacer(Modifier.width(spacing))

        Crossfade(targetState = list[currentIndex], label = "stepper_value") { v ->
            Box(
                modifier = Modifier
                    .height(height)
                    .wrapContentWidth()
                    .semantics { contentDescription = "Value $v" },
                contentAlignment = Alignment.Center
            ) { label(v) }
        }

        Spacer(Modifier.width(spacing))

        StepperButton(
            enabled = canInc,
            onClick = { bump(+1) },
            onLongPressRepeat = { bump(+1) },
            minWidth = buttonMinWidth,
            contentDescription = "Increase"
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

/** Bouton compact avec auto-repeat au long-press. */
@Composable
private fun StepperButton(
    enabled: Boolean,
    onClick: () -> Unit,
    onLongPressRepeat: () -> Unit,
    minWidth: Dp,
    contentDescription: String,
    content: @Composable RowScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    var repeatJob by remember { mutableStateOf<Job?>(null) }

    FilledTonalButton(
        onClick = {
            repeatJob?.cancel()
            onClick()
        },
        enabled = enabled,
        interactionSource = interaction,
        contentPadding = PaddingValues(horizontal = 0.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .defaultMinSize(minWidth = minWidth, minHeight = 36.dp)
            .semantics { this.contentDescription = contentDescription }
    ) { content() }

    // Auto-repeat: démarre après 400 ms, puis accélère
    LaunchedEffect(interaction, enabled) {
        if (!enabled) return@LaunchedEffect
        interaction.interactions.collect { event ->
            when (event) {
                is PressInteraction.Press -> {
                    repeatJob?.cancel()
                    repeatJob = scope.launch {
                        delay(400)
                        var interval = 120L
                        while (isActive) {
                            onLongPressRepeat()
                            delay(interval)
                            if (interval > 60) interval -= 10
                        }
                    }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    repeatJob?.cancel(); repeatJob = null
                }
            }
        }
    }
}
