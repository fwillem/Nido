package com.example.nido.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.example.nido.utils.println
import com.example.nido.utils.Constants.NB_OF_DISCARDED_CARDS_TO_SHOW




@Composable
fun MatView(
    playmat: SnapshotStateList<Card>?,
    discardPile: SnapshotStateList<Card>,
    selectedCards: SnapshotStateList<Card>,
    onPlayCombination: (List<Card>, Card?) -> Unit,  // ✅ Keep the callback!
    cardWidth: Dp,
    cardHeight: Dp,
) {
    TRACE(DEBUG) {
        "🟦 Recomposing MatView : \n" +
                "Playmat : ${playmat?.joinToString { "${it.value} ${it.color}" } ?: "Empty"}\n" +
                "DiscardPile : ${discardPile.joinToString { "${it.value} ${it.color}" }}\n" +
                "SelectedCards : ${selectedCards.joinToString { "${it.value} ${it.color}" }} \n" }

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
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (card in selectedCards) {
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
            Text("Playmat:", color = Color.White)
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                playmat?.let { cards ->
                    for (card in cards) {
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
            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Discard Pile:", color = Color.Red)
                Text("${discardPile.size}",color = Color.Red)
            }
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                for (card in discardPile.takeLast(NB_OF_DISCARDED_CARDS_TO_SHOW)) {
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
                    onClick = { onPlayCombination(selectedCards.toList(), playmat?.firstOrNull()) }, // ✅ Play the combination
                    /* TODO Here we shall ask the user the card he wants to keep */
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        "Play Combination",
                        fontSize = 8.sp,
                        lineHeight = 8.sp
                        )
                }



/*

                        @Composable
                        fun ActionButtonsView(actions: Map<String, () -> Unit>) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                actions.forEach { (label, action) ->
                                    Button(
                                        onClick = action,
                                        modifier = Modifier
                                            .height(16.dp)
                                            .padding(horizontal = 2.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text(
                                            label,
                                            fontSize = 8.sp,
                                            lineHeight = 8.sp
                                        )
                                    }
                                }
                            }
                        }


 */


            }
        }
    }
}
