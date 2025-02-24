package com.example.nido.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nido.data.model.Card

@Composable
fun MatView(
    playmat: SnapshotStateList<Card>,
    discardPile: SnapshotStateList<Card>,
    cardWidth: Dp,
    cardHeight: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Playmat Section (75% Width)
            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .background(Color(0xFF228B22), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                LazyRow(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items(playmat.size) { index ->
                        CardView(
                            card = playmat[index],
                            modifier = Modifier
                                .width(cardWidth)
                                .height(cardHeight)
                        )
                    }
                }
            }
            // Discard Pile Section (25% Width)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.Gray, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                DiscardPileView(
                    discardPile = discardPile,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight
                )
            }
        }
    }
}
