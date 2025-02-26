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

@Composable
fun HandView(
    hand: Hand,
    cardWidth: Dp,
    cardHeight: Dp,
    sortMode: SortMode,
    onDoubleClick: () -> Unit,
    onPlayCombination: (List<Card>) -> Unit,  // ✅ Supports multiple cards (a combination)
    onSelectCard: (List<Card>) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedCards = remember { mutableStateListOf<Card>() }  // ✅ Track selected combination

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
                val card = currentHand[index]
                var offsetY by remember { mutableStateOf(0f) }
                var dragging by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .shadow(if (dragging) 8.dp else 0.dp)
                        .offset(y = offsetY.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    dragging = true
                                    println("🔹 Dragging card: ${card.value}, ${card.color}")
                                },
                                onDragEnd = {
                                    dragging = false
                                    if (offsetY < -cardHeight.toPx() / 2) {  // ✅ Drag threshold reached
                                        coroutineScope.launch {
                                            if (index < hand.cards.size) {
                                                val removedCard = hand.removeCard(index)
                                                if (removedCard != null) {
                                                    println("✅ Successfully selected card: ${removedCard.value}, ${removedCard.color}")
                                                    selectedCards.add(removedCard)  // ✅ Collect selected cards
                                                    onSelectCard(selectedCards)
                                                } else {
                                                    println("❌ ERROR: Failed to select card at index: $index")
                                                }
                                            } else {
                                                println("❌ ERROR: Index out of bounds: $index, Hand size: ${hand.cards.size}")
                                            }
                                        }
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

    // ✅ Play button for confirming selected cards
    if (selectedCards.isNotEmpty()) {
        Button(
            onClick = {
                println("▶️ Playing combination: ${selectedCards.map { "${it.value}, ${it.color}" }}")
                onPlayCombination(selectedCards)  // ✅ Send selected combination
                selectedCards.clear()  // ✅ Clear after playing
            }
        ) {
            Text("Play Combination")
        }
    }
}
