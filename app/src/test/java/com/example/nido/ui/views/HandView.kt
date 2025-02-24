package com.example.nido.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.nido.CardView
import com.example.nido.SortMode
import com.example.nido.moveItem
import com.example.nido.sortedByComplexCriteria
import com.example.nido.sortedByMode


@Composable
fun HandView(
    hand: Hand,
    cardWidth: Dp,
    cardHeight: Dp,
    sortMode: SortMode,
    onDoubleClick: () -> Unit
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var targetIndex by remember { mutableStateOf<Int?>(null) }

    val sortedCards = remember(sortMode) {
        derivedStateOf {
            if (sortMode == SortMode.COLOR)
                hand.cards.sortedByComplexCriteria()
            else
                hand.cards.sortedByMode(sortMode)
        }
    }.value

    val cardWidthPx = with(LocalDensity.current) { cardWidth.toPx() }

    LaunchedEffect(draggedIndex, dragOffsetX, sortMode) {
        if (sortMode == SortMode.FIFO && draggedIndex != null) {
            val rawIndexShift = (dragOffsetX / cardWidthPx).toInt()
            val potentialTargetIndex = draggedIndex!! + rawIndexShift
            targetIndex = when {
                dragOffsetX > 0 -> (potentialTargetIndex + 1).coerceIn(0, hand.cards.size)
                dragOffsetX < 0 -> potentialTargetIndex.coerceIn(0, hand.cards.size - 1)
                else -> potentialTargetIndex
            }
        } else {
            targetIndex = null
        }
    }

    // Instead of filling the entire available space, we only wrap the content.
    Box(
        modifier = Modifier
            .wrapContentSize()   // Key: size only as needed by its children
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onDoubleClick() }) },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.wrapContentWidth(), // Row's width equals the sum of its children
            horizontalArrangement = Arrangement.spacedBy(0.dp) // No extra spacing between items
        ) {
            sortedCards.forEachIndexed { sortedIndex, card ->
                if (targetIndex != null &&
                    sortedIndex == targetIndex &&
                    sortMode == SortMode.FIFO
                ) {
                    // Optional insertion marker (red line)
                    Box(
                        modifier = Modifier
                            .height(cardHeight)
                            .width(6.dp)
                            .background(Color.Red)
                    )
                }
                val actualIndex = hand.cards.indexOf(card)
                Box(
                    modifier = Modifier
                        .zIndex(if (draggedIndex == actualIndex) 1f else 0f)
                        .graphicsLayer {
                            if (draggedIndex == actualIndex) {
                                alpha = 0.5f
                                translationX = dragOffsetX
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    if (sortMode == SortMode.FIFO) {
                                        draggedIndex = actualIndex
                                    }
                                },
                                onDragEnd = {
                                    if (sortMode == SortMode.FIFO &&
                                        draggedIndex != null &&
                                        targetIndex != null
                                    ) {
                                        val adjustedTarget = if (dragOffsetX > 0)
                                            (targetIndex!! - 1).coerceIn(0, hand.cards.size - 1)
                                        else targetIndex!!
                                        hand.cards.moveItem(draggedIndex!!, adjustedTarget)
                                    }
                                    draggedIndex = null
                                    dragOffsetX = 0f
                                },
                                onDragCancel = {
                                    draggedIndex = null
                                    dragOffsetX = 0f
                                },
                                onDrag = { _, dragAmount ->
                                    if (sortMode == SortMode.FIFO && draggedIndex != null) {
                                        dragOffsetX += dragAmount.x
                                    }
                                }
                            )
                        }
                ) {
                    CardView(
                        card,
                        Modifier
                            .width(cardWidth)
                            .height(cardHeight)
                    )
                }
            }
            if (targetIndex != null &&
                targetIndex == hand.cards.size &&
                sortMode == SortMode.FIFO
            ) {
                Box(
                    modifier = Modifier
                        .height(cardHeight)
                        .width(6.dp)
                        .background(Color.Red)
                )
            }
        }
    }
}
