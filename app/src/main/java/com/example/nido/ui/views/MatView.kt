package com.example.nido.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nido.data.model.Card
import com.example.nido.ui.theme.NidoColors
import com.example.nido.ui.views.CardView
import com.example.nido.ui.views.DiscardPileView
import androidx.compose.ui.graphics.Color

@Composable
fun MatView(
    playmat: SnapshotStateList<Card>?,
    discardPile: SnapshotStateList<Card>,
    selectedCards: SnapshotStateList<Card>,
    onPlayCombination: (List<Card>) -> Unit,  // ✅ Keep the callback!
    cardWidth: Dp,
    cardHeight: Dp,
) {
    println("🟦 MatView - Playmat contains: ${playmat?.joinToString { "${it.value} ${it.color}" } ?: "Empty"}")
    println("🟥 MatView - DiscardPile contains: ${discardPile.joinToString { "${it.value} ${it.color}" }}")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            // **Selected Cards Row**
            if (selectedCards.isNotEmpty()) {
                Text("Selected Combination:", color = Color.Yellow)
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (card in selectedCards) {
                        println("🟡 Showing Selected Card: ${card.value}, ${card.color}")
                        CardView(
                            card = card,
                            modifier = Modifier
                                .width(cardWidth)
                                .height(cardHeight)
                                .padding(4.dp)
                        )
                    }
                }
            }

            // **Playmat Row**
            Text("Current Playmat:", color = Color.White)
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                playmat?.let { cards ->
                    for (card in cards) {
                        println("🟢 Showing Playmat Card: ${card.value}, ${card.color}")
                        CardView(
                            card = card,
                            modifier = Modifier
                                .width(cardWidth)
                                .height(cardHeight)
                        )
                    }
                }

            }

            // **Discard Pile**
            Text("Discard Pile:", color = Color.Red)
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                for (card in discardPile) {
                    println("🟥 Showing Discard Card: ${card.value}, ${card.color}")
                    CardView(
                        card = card,
                        modifier = Modifier
                            .width(cardWidth)
                            .height(cardHeight)
                    )
                }
            }

            // **Play Button - Only when selection is not empty**
            if (selectedCards.isNotEmpty()) {
                Button(
                    onClick = { onPlayCombination(selectedCards.toList()) }, // ✅ Play the combination
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Play Combination")
                }
            }
        }
    }
}
