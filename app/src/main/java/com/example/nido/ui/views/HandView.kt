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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nido.data.model.Card
import com.example.nido.data.model.Hand
import com.example.nido.utils.SortMode
import kotlinx.coroutines.launch
import com.example.nido.ui.views.CardView
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.example.nido.utils.println
import com.example.nido.data.model.CardResources.backCoverCard

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

    TRACE(VERBOSE) { "Recomposing HandView : ${hand.cards}" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight + 20.dp)
            .background(Color(0xFF006400))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val currentHand = hand.cards.toList()

            currentHand.indices.forEach { index ->
                val card = currentHand[index] // 🔄 Capturing card from currentHand once here
                var offsetY by remember { mutableStateOf(0f) }
                var dragging by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .shadow(if (dragging) 8.dp else 0.dp)
                        .offset(y = offsetY.dp)
                        .pointerInput(card) { // 🔄 Changed key from Unit to card so the lambda updates when card changes
                            detectDragGestures(
                                onDragStart = {
                                    dragging = true
                                    // 🔄 Using the captured 'card' rather than reading from hand.cards[index]
                                    TRACE(VERBOSE, tag = "HandView:onDragStart1") { "Dragging card: ${card.value}, ${card.color}, index = $index" }
                                    // 🔄 Removed redundant log that accessed hand.cards[index]
                                },
                                onDragEnd = {
                                    dragging = false
                                    TRACE(VERBOSE, tag = "HandView:onDragEnd") { "Drag End card: ${card.value}, ${card.color}, index = $index" }

                                    if (offsetY < -cardHeight.toPx() / 2) {  // ✅ Drag threshold reached
                                        coroutineScope.launch {
                                            // 🔄 Removing the card using the captured card instance instead of by index
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
                                    // 🔄 Changed tag to "onDrag" for clarity
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

