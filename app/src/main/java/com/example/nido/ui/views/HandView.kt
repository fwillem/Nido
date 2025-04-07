package com.example.nido.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nido.data.model.Card
import com.example.nido.data.model.Hand
import com.example.nido.utils.sortedByMode
import com.example.nido.utils.SortMode
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.example.nido.ui.theme.NidoColors
import com.example.nido.ui.views.CardView

@Composable
fun HandView(
    hand: Hand,
    cardWidth: Dp,
    cardHeight: Dp,
    sortMode: SortMode,
    onDoubleClick: () -> Unit,
    onSortMode: () -> Unit,
    onSelectCard: (Card) -> Unit
) {
    val density = LocalDensity.current

    TRACE(VERBOSE) { "Recomposing HandView: ${hand.cards}" }

    Box(
        modifier = Modifier
            .background(NidoColors.HandViewBackground2)
            .padding(bottom = 8.dp)
    ) {
        val sortedCards by remember(hand.cards, sortMode) {
            derivedStateOf { hand.cards.sortedByMode(sortMode) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            sortedCards.forEachIndexed { index, card ->
                var offsetY by remember { mutableStateOf(0f) }
                var dragging by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .shadow(if (dragging) 8.dp else 0.dp)
                        .offset(y = with(density) { offsetY.toDp() })
                        .pointerInput(card) {
                            detectTapGestures(
                                onDoubleTap = { onDoubleClick() }
                            )
                        }
                        .pointerInput(card) {
                            detectDragGestures(
                                onDragStart = {
                                    dragging = true
                                    TRACE(VERBOSE, tag = "HandView:onDragStart") {
                                        "Dragging card: ${card.value}, ${card.color}, index = $index"
                                    }
                                },
                                onDragEnd = {
                                    dragging = false
                                    TRACE(VERBOSE, tag = "HandView:onDragEnd") { "Drag End card: ${card.value}, ${card.color}, index = $index" }
                                    // Drag threshold reached (cardHeight converted to pixels)
                                    if (offsetY < -cardHeight.run { with(density) { toPx() } } / 2) {
                                        onSelectCard(card)
                                    } else {
                                        TRACE(VERBOSE, tag = "HandView:onDragEnd") { "Drag threshold not reached" }
                                    }
                                    offsetY = 0f
                                },
                                onDragCancel = {
                                    dragging = false
                                    offsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    offsetY += dragAmount.y
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
