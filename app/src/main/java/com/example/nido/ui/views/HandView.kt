package com.example.nido.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity  // 🚀 Added import for LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nido.data.model.Card
import com.example.nido.data.model.Hand
import com.example.nido.utils.sortedByMode
import com.example.nido.utils.SortMode
import com.example.nido.ui.views.CardView
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.example.nido.utils.println
import com.example.nido.data.model.CardResources.backCoverCard
import com.example.nido.utils.Constants
import com.example.nido.ui.theme.NidoColors
import kotlinx.coroutines.launch

@Composable
fun HandView(
    hand: Hand,
    cardWidth: Dp,
    cardHeight: Dp,
    sortMode: SortMode,
    onDoubleClick: () -> Unit,
    onSelectCard: (Card) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    // 🚀 NEW: Retrieve the current density to convert between px and dp
    val density = LocalDensity.current

    TRACE(VERBOSE) { "Recomposing HandView : ${hand.cards}" }

    Box(
        modifier = Modifier
            .background(NidoColors.HandViewBackground2)
            .padding(bottom = 8.dp)
    ) {
        // 🚀 NEW: Use sorted cards based on sortMode with the restored sorting logic
        val sortedCards by remember(hand.cards, sortMode) {
            derivedStateOf {
                hand.cards.sortedByMode(sortMode)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            // 🚀 Use the sortedCards list for iteration
            sortedCards.indices.forEach { index ->
                val card = sortedCards[index] // Use sortedCards instead of currentHand
                var offsetY by remember { mutableStateOf(0f) }
                var dragging by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .shadow(if (dragging) 8.dp else 0.dp)
                        // 🚀 Convert offsetY from pixels to dp using density
                        .offset(y = with(density) { offsetY.toDp() })
                        .pointerInput(card) { // Use card as key so the lambda updates when card changes
                            detectDragGestures(
                                onDragStart = {
                                    dragging = true
                                    TRACE(VERBOSE, tag = "HandView:onDragStart1") { "Dragging card: ${card.value}, ${card.color}, index = $index" }
                                },
                                onDragEnd = {
                                    dragging = false
                                    TRACE(VERBOSE, tag = "HandView:onDragEnd") { "Drag End card: ${card.value}, ${card.color}, index = $index" }

                                    // ✅ Drag threshold reached (converted cardHeight to pixels)
                                    if (offsetY < -cardHeight.run { with(density) { toPx() } } / 2) {
                                        coroutineScope.launch {
                                            if (hand.cards.contains(card)) {
                                                val removed = hand.removeCard(card)
                                                if (removed) {
                                                    TRACE(DEBUG, tag = "HandView:onDragEnd") { "✅ Successfully selected card: ${card.value}, ${card.color}" }
                                                    onSelectCard(card)
                                                } else {
                                                    TRACE(ERROR, tag = "HandView:onDragEnd") { "Failed to remove card: ${card.value}, ${card.color}" }
                                                }
                                            } else {
                                                TRACE(ERROR, tag = "HandView:onDragEnd") { "Card not found in hand: ${card.value}, ${card.color}" }
                                            }
                                        }
                                    } else {
                                        TRACE(VERBOSE, tag = "HandView:onDragEnd") { "Drag threshold not reached" }
                                    }
                                    offsetY = 0f
                                },
                                onDragCancel = {
                                    TRACE(VERBOSE, tag = "HandView:onDragCancel") { "Drag canceled" }
                                    dragging = false
                                    offsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    offsetY += dragAmount.y
                                    TRACE(VERBOSE, tag = "HandView:onDrag") { "dragAmount: $dragAmount" }
                                }
                            )
                        }
                ) {
                    CardView(
                        card = card,
                        modifier = Modifier.size(cardWidth, cardHeight)
                    )
                }
            }
        }
    }
}
