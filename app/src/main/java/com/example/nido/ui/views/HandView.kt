package com.example.nido.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures  // 🚀 NEW: Import detectTapGestures (stable)
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity  // 🚀 Added import for LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Hand
import com.example.nido.game.FakeGameManager
import com.example.nido.game.GameScreens
import com.example.nido.game.GameState
import com.example.nido.game.GameViewModel
import com.example.nido.game.LocalPlayer
import com.example.nido.game.ai.AIPlayer
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.screens.MainScreen
import com.example.nido.ui.views.CardView
import com.example.nido.utils.sortedByMode
import com.example.nido.utils.SortMode
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.example.nido.ui.theme.NidoColors
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.utils.Constants
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
    // Retrieve the current density to convert between px and dp.
    val density = LocalDensity.current

    TRACE(VERBOSE) { "Recomposing HandView : ${hand.cards}" }

    Box(
        modifier = Modifier
            .background(NidoColors.HandViewBackground2)
            .padding(bottom = 8.dp)
    ) {
        // Use sorted cards based on sortMode with your sorting logic.
        val sortedCards by remember(hand.cards, sortMode) {
            derivedStateOf { hand.cards.sortedByMode(sortMode) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            // Iterate over the sortedCards list.
            sortedCards.indices.forEach { index ->
                val card = sortedCards[index] // Use sortedCards order.
                var offsetY by remember { mutableStateOf(0f) }
                var dragging by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .shadow(if (dragging) 8.dp else 0.dp)
                        // Convert offsetY from pixels to dp.
                        .offset(y = with(density) { offsetY.toDp() })
                        // 🚀 NEW: Use a pointerInput to detect double taps (using stable detectTapGestures).
                        .pointerInput(card) {
                            detectTapGestures(
                                onDoubleTap = { onDoubleClick() }
                            )
                        }
                        // 🚀 NEW: Then chain another pointerInput for drag gestures.
                        .pointerInput(card) {
                            detectDragGestures(
                                onDragStart = {
                                    dragging = true
                                    TRACE(VERBOSE, tag = "HandView:onDragStart1") {
                                        "Dragging card: ${card.value}, ${card.color}, index = $index"
                                    }
                                },
                                onDragEnd = {
                                    dragging = false
                                    TRACE(VERBOSE, tag = "HandView:onDragEnd") {
                                        "Drag End card: ${card.value}, ${card.color}, index = $index"
                                    }
                                    // Drag threshold reached (cardHeight converted to pixels).
                                    if (offsetY < -cardHeight.run { with(density) { toPx() } } / 2) {
                                        coroutineScope.launch {
                                            if (hand.cards.contains(card)) {
                                                val removed = hand.removeCard(card)
                                                if (removed) {
                                                    TRACE(DEBUG, tag = "HandView:onDragEnd") {
                                                        "✅ Successfully selected card: ${card.value}, ${card.color}"
                                                    }
                                                    onSelectCard(card)
                                                } else {
                                                    TRACE(ERROR, tag = "HandView:onDragEnd") {
                                                        "Failed to remove card: ${card.value}, ${card.color}"
                                                    }
                                                }
                                            } else {
                                                TRACE(ERROR, tag = "HandView:onDragEnd") {
                                                    "Card not found in hand: ${card.value}, ${card.color}"
                                                }
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

@Preview(
    name = "Landscape HandView Preview",
    widthDp = 800, // wider than it is tall
    heightDp = 400, // adjust as needed
    showBackground = true
)
@Composable
fun PreviewHandView() {
    NidoTheme {
        CompositionLocalProvider(LocalGameManager provides FakeGameManager()) {
            HandView(
                hand = Hand(mutableStateListOf(
                    Card(1, "RED"),
                    Card(2, "PINK")
                )),
                cardWidth = Constants.CARD_ON_HAND_WIDTH.dp,
                cardHeight = Constants.CARD_ON_HAND_HEIGHT.dp,
                sortMode = SortMode.FIFO, // Use a default sort mode value
                onDoubleClick = { },
                onSelectCard = { /* no-op for preview */ }
            )
        }
    }
}
