package com.example.nido.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Modes d’animation IA -> durée en millisecondes.
 */
enum class AITimerPreset(val label: String, val durationMs: Int) {
    NORMAL("Normale", 1800),
    FAST("Rapide", 1200),
    TURBO("Turbo", 900);

    companion object {
        /** Convertit une durée arbitraire (ms) vers le preset le plus proche. */
        fun fromDuration(durationMs: Int): AITimerPreset =
            values().minBy { preset -> abs(preset.durationMs - durationMs) }
    }
}

/**
 * 3 boutons pleins (même look que tes autres boutons).
 * Le bouton sélectionné est en Primary, les autres en PrimaryContainer.
 */
@Composable
fun AITimerSelector(
    selected: AITimerPreset,
    onSelected: (AITimerPreset) -> Unit,
    modifier: Modifier = Modifier,
    height: Int = 40
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AITimerPreset.values().forEach { preset ->
            val isSelected = preset == selected
            Button(
                onClick = { onSelected(preset) },
                modifier = Modifier
                    .weight(1f)
                    .height(height.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(preset.label)
            }
        }
    }
}
