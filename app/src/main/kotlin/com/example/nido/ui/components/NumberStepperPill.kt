package com.example.nido.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip

@Composable
fun NumberStepperPill(
    value: Int,
    values: List<Int>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    minWidth: Dp = 200.dp,
    wrapAround: Boolean = false,
    label: @Composable (Int) -> Unit = {
        Text(it.toString(), style = MaterialTheme.typography.titleMedium)
    }
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

    Surface(
        modifier = modifier
            .defaultMinSize(minWidth = minWidth, minHeight = height)
            .semantics { role = androidx.compose.ui.semantics.Role.Button; contentDescription = "Stepper" },
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .height(height)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Zone "-"
            PillSide(
                enabled = canDec,
                onClick = { bump(-1) },
                onLongPressRepeat = { bump(-1) },
                modifier = Modifier
                    .fillMaxHeight()
                    .width(height)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease")
            }

            // Valeur
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) { label(list[currentIndex]) }

            // Zone "+"
            PillSide(
                enabled = canInc,
                onClick = { bump(+1) },
                onLongPressRepeat = { bump(+1) },
                modifier = Modifier
                    .fillMaxHeight()
                    .width(height)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Increase")
            }
        }
    }
}

@Composable
private fun PillSide(
    enabled: Boolean,
    onClick: () -> Unit,
    onLongPressRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp)) // garde la forme ovale
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(                       // ⟵ rend la zone interactive
                enabled = enabled,
                interactionSource = interaction,
                indication = null,            // pas d’ondulation, aspect bouton unique
                onClick = { onClick() }
            )
            .semantics { this.contentDescription = "pill-side" },
        contentAlignment = Alignment.Center
    ) { content() }

    // Auto‑repeat au long‑press (maintenu)
    LaunchedEffect(enabled, interaction) {
        if (!enabled) return@LaunchedEffect
        interaction.interactions.collect { e ->
            when (e) {
                is PressInteraction.Press -> {
                    job?.cancel()
                    job = scope.launch {
                        // onClick déjà déclenché ci‑dessus au tap
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
                    job?.cancel(); job = null
                }
            }
        }
    }
}


// 🔍 Preview pour visualiser dans Android Studio
@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
fun NumberStepperPillPreview() {
    var value by remember { mutableStateOf(10) }
    val steps = (0..100 step 10).toList()
    NumberStepperPill(
        value = value,
        values = steps,
        onValueChange = { value = it },
        modifier = Modifier.padding(16.dp)
    )
}
