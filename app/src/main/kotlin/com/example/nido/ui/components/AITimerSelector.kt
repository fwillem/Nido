package com.example.nido.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

enum class AITimerPreset(val label: String, val durationMs: Int) {
    NORMAL("Normale", 1800),
    FAST("Rapide", 1200),
    TURBO("Turbo", 900);

    companion object {
        fun fromDuration(durationMs: Int): AITimerPreset =
            values().minBy { abs(it.durationMs - durationMs) }
    }
}

/**
 * Segmented control "pilule" d'un seul tenant :
 * - fond SurfaceVariant
 * - segment sélectionné en Primary (texte onPrimary)
 * - séparateurs discrets entre segments
 * - aucun OutlinedButton => pas de liseré blanc
 */
@Composable
fun AITimerSelector(
    selected: AITimerPreset,
    onSelected: (AITimerPreset) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp
) {
    val outerShape = RoundedCornerShape(22.dp)
    val bgContainer = MaterialTheme.colorScheme.surfaceVariant
    val fgUnselected = MaterialTheme.colorScheme.onSurfaceVariant
    val bgSelected = MaterialTheme.colorScheme.primary
    val fgSelected = MaterialTheme.colorScheme.onPrimary
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    Surface(
        modifier = modifier
            .height(height)
            .fillMaxWidth(),
        shape = outerShape,
        color = bgContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp), // petit inset pour éviter tout liseré de clipping
            verticalAlignment = Alignment.CenterVertically
        ) {
            val all = AITimerPreset.values()
            all.forEachIndexed { index, preset ->
                val isSelected = preset == selected
                val segShape: Shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                    all.lastIndex -> RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
                    else -> RectangleShape
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(segShape)
                        .background(if (isSelected) bgSelected else bgContainer)
                        .clickable { onSelected(preset) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = preset.label,
                        color = if (isSelected) fgSelected else fgUnselected,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // séparateur fin entre segments (sauf après le dernier)
                if (index < all.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(dividerColor)
                    )
                }
            }
        }
    }
}
