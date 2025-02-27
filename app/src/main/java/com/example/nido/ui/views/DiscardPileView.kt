package com.example.nido.ui.views

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.nido.data.model.Card


@Composable
fun DiscardPileView(discardPile: List<Card>, cardWidth: Dp, cardHeight: Dp) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .width(cardWidth * 3)
            .height(cardHeight)
            .border(2.dp, Color.White, RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (discardPile.isEmpty()) {
            Text(
                text = "DISCARD",
                fontSize = 12.sp,
                color = Color.White,
                modifier = Modifier.graphicsLayer(rotationZ = -45f)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val maxCardsToShow = 5
                val overlapFraction = 0.67f
                val displayedCards = discardPile.takeLast(maxCardsToShow)
                displayedCards.forEachIndexed { index, card ->
                    val cardWidthPx = with(density) { cardWidth.toPx() }
                    val offsetX = (-index * (cardWidthPx * overlapFraction))
                    Box(
                        modifier = Modifier
                            .zIndex(index.toFloat())
                            .graphicsLayer(translationX = offsetX)
                    ) {
                        CardView(
                            card = card,
                            modifier = Modifier
                                .width(cardWidth)
                                .height(cardHeight)
                        )
                    }
                }
            }
        }
    }
}
