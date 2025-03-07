package com.example.nido.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.CardColor // Merged enum with uiColor property
import com.example.nido.game.rules.GameRules.isValidMove
import com.example.nido.utils.Constants.NB_OF_DISCARDED_CARDS_TO_SHOW
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.example.nido.events.AppEvent
import com.example.nido.game.GameManager
import com.example.nido.game.GameViewModel
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*




@Composable
fun MatView(
    playmat: SnapshotStateList<Card>?,
    discardPile: SnapshotStateList<Card>,
    selectedCards: SnapshotStateList<Card>,
    onPlayCombination: (List<Card>, Card?) -> Unit,  // Callback for committing a move
    onWithdrawCards: (List<Card>) -> Unit,             // Callback for withdrawing cards
    cardWidth: Dp,
    cardHeight: Dp,
) {

    TRACE(DEBUG) {
        "🟦 Recomposing MatView : \n" +
                "Playmat : ${playmat?.joinToString { "${it.value} ${it.color}" } ?: "Empty"}\n" +
                "DiscardPile : ${discardPile.joinToString { "${it.value} ${it.color}" }}\n" +
                "SelectedCards : ${selectedCards.joinToString { "${it.value} ${it.color}" }} \n"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
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
                Text("${discardPile.size}", color = Color.Red)
            }
            /*
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

             */


            // **Play Button - Only when selection is not empty**
            if (selectedCards.isNotEmpty()) {
                // Create a temporary combination from playmat (or empty if playmat is null)
                val currentCombination = if (playmat != null) Combination(playmat) else Combination(mutableListOf())
                if (isValidMove(currentCombination, Combination(selectedCards))) {
                    Button(
                        onClick = {
                            // Determine candidate cards from the playmat.
                            val candidateCards = playmat?.toList() ?: emptyList()
                            when {
                                candidateCards.isEmpty() -> {
                                    // More than one candidate: dispatch a dialog event via GameManager.
                                    TRACE(DEBUG) { "No card to keep" }
                                    // If no candidate cards, commit move with no card to keep.
                                    onPlayCombination(selectedCards.toList(), null)
                                    selectedCards.clear()
                                }
                                candidateCards.size == 1 -> {

                                    // Only one candidate: commit immediately.
                                    TRACE(DEBUG) { "Only one candidate: commit immediately ${candidateCards.first()}" }
                                    // If only one candidate, commit immediately.
                                    onPlayCombination(selectedCards.toList(), candidateCards.first())
                                    selectedCards.clear()
                                }
                                else -> {

                                    // More than one candidate: dispatch a dialog event via GameManager.
                                    TRACE(DEBUG) { "Several candidates: ${candidateCards.joinToString { "${it.value} ${it.color}" }}" }

                                    GameManager.setDialogEvent(
                                        AppEvent.GameEvent.CardSelection(
                                            candidateCards = candidateCards,
                                            selectedCards = selectedCards.toList(),
                                            onConfirm = { chosenCard ->
                                                onPlayCombination(selectedCards.toList(), chosenCard)
                                                selectedCards.clear()
                                                GameManager.clearDialogEvent()
                                            },
                                            onCancel = {
                                                GameManager.clearDialogEvent()
                                            }
                                        )
                                    )


                                }
                            }
                        },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Play Combination", fontSize = 8.sp, lineHeight = 8.sp)
                    }
                } else {
                    Button(
                        onClick = { onWithdrawCards(selectedCards.toList()) },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Remove Combination:", fontSize = 8.sp, lineHeight = 8.sp)
                    }
                }
            }
        }
    }

}
