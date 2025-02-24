package com.example.nido.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nido.SortMode


@Composable
fun HandViewDebugView(
    hand: Hand,
    cardWidth: Dp,
    cardHeight: Dp,
    sortMode: SortMode, // Not used here, but kept for prototype compatibility
    onDoubleClick: () -> Unit
) {
    // The outer Box fills the full width (since NidoApp places HandView inside a Box that fills width)
    // and centers its content vertically.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.CenterVertically)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onDoubleClick() })
            },
        contentAlignment = Alignment.Center
    ) {
        // Inner Box sizes itself only to the content (the Row)
        Box(modifier = Modifier.wrapContentSize(Alignment.Center)) {
            // Row with wrapContentWidth() ensures its width is exactly the sum of its children
            Row(
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp) // No extra spacing
            ) {
                // For each card in the hand, display a magenta box
                hand.cards.forEach {
                    Box(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(cardHeight)
                            .background(Color.Magenta)
                    ) {

                    }
                }
            }
        }
    }
}

