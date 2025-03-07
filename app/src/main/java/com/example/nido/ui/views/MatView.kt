package com.example.nido.ui.views

import androidx.compose.foundation.background
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nido.ui.views.MatView

import androidx.compose.ui.unit.dp
import com.example.nido.ui.theme.NidoColors
import com.example.nido.ui.views.MatView

@Composable
fun MatView(
    playmat: SnapshotStateList<Card>?,
    discardPile: SnapshotStateList<Card>,
    selectedCards: SnapshotStateList<Card>,
    onPlayCombination: (List<Card>, Card?) -> Unit,  // Callback for committing a move
    onWithdrawCards: (List<Card>) -> Unit,             // Callback for withdrawing cards
    onSkip: () -> Unit,                                // Callback for skipping a turn
    cardWidth: Dp,
    cardHeight: Dp,
) {

    TRACE(DEBUG) {
        "🟦 Recomposing MatView : \n" +
                "Playmat : ${playmat?.joinToString { "${it.value} ${it.color}" } ?: "Empty"}\n" +
                "DiscardPile : ${discardPile.joinToString { "${it.value} ${it.color}" }}\n" +
                "SelectedCards : ${selectedCards.joinToString { "${it.value} ${it.color}" }} \n"
    }
        Row(modifier = Modifier
            .fillMaxSize(),
        ) {
            // Left section: Selected Cards

            Box(modifier = Modifier
                .weight(1f)
                .background(NidoColors.SelectedCardBackground)
                .fillMaxSize(),
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

            }

            // Right section: Playmat and Button

            Box(modifier = Modifier
                .weight(1f)
                .background(NidoColors.PlaymatBackground)
                .fillMaxSize(),
            ) {

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
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NidoColors.PlayMatButtonBackground.copy(alpha = 0.8f),
                                contentColor = Color.White
                            )

                        ) {
                            Text("Play", fontSize = 12.sp, lineHeight = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { onWithdrawCards(selectedCards.toList()) },
                            modifier = Modifier
                               .align(Alignment.CenterEnd)
                                .padding(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NidoColors.PlayMatButtonBackground.copy(alpha = 0.8f),
                                contentColor = Color.White
                            )



                        ) {
                            Text("Remove", fontSize = 12.sp, lineHeight = 12.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = { onSkip() },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NidoColors.PlayMatButtonBackground.copy(alpha = 0.8f),
                            contentColor = Color.White
                        )


                    ) {
                        Text("Skip", fontSize = 12.sp, lineHeight = 12.sp)
                    }

                }


            }

            // **Discard Pile is discarded !
            /*
            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Discard Pile:", color = Color.Red)
                Text("${discardPile.size}", color = Color.Red)
            }
             */

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
/*
            Box(modifier = Modifier
                .weight(0.1f)
            ) {
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

 */

        }


}



@Preview(
    name = "MatView - 2 on Playmat, 3 on Selected",
    showBackground = true,
    widthDp = 800,
    heightDp = 400
)

@Composable
fun PreviewMatViewScenario1() {
    // 🚀 Create dummy SnapshotStateLists for scenario 1:
    val playmatCards = remember { mutableStateListOf<Card>().apply {
        add(Card(2, "RED"))
        add(Card(2, "RED"))
        add(Card(2, "RED"))

        add(Card(3, "GREEN"))
    } }
    val selectedCards = remember { mutableStateListOf<Card>().apply {
        add(Card(4, "BLUE"))
        add(Card(5, "MOCHA"))
        add(Card(6, "PINK"))
    } }
    // Using an empty discard pile for simplicity:
    val discardPile = remember { mutableStateListOf<Card>() }

    // 🚀 Dummy lambdas for button callbacks:
    val onPlayCombination: (List<Card>, Card?) -> Unit = { cards, cardToKeep ->
        // No-op for preview
    }
    val onWithdrawCards: (List<Card>) -> Unit = { cards ->
        // No-op for preview
    }

    val onSkip: () -> Unit = {
        // No-op for preview
    }

    MatView(
        playmat = playmatCards,
        discardPile = discardPile,
        selectedCards = selectedCards,
        onPlayCombination = onPlayCombination,
        onWithdrawCards = onWithdrawCards,
        onSkip = onSkip,
        cardWidth = 80.dp,   // 🚀 Example dimensions
        cardHeight = 120.dp  // 🚀 Example dimensions
    )
}
/*
@Preview(
    name = "MatView - 4 on Playmat, 5 on Selected",
    showBackground = true,
    widthDp = 800,
    heightDp = 400
)

@Composable
fun PreviewMatViewScenario2() {
    // 🚀 Create dummy SnapshotStateLists for scenario 2:
    val playmatCards = remember { mutableStateListOf<Card>().apply {
        add(Card(2, "RED"))
        add(Card(3, "GREEN"))
        add(Card(4, "BLUE"))
        add(Card(5, "MOCHA"))
    } }
    val selectedCards = remember { mutableStateListOf<Card>().apply {
        add(Card(6, "PINK"))
        add(Card(7, "ORANGE"))
        add(Card(8, "MOCHA"))
        add(Card(9, "GREEN"))
        add(Card(10, "RED"))
    } }
    val discardPile = remember { mutableStateListOf<Card>() }

    // 🚀 Dummy lambdas for button callbacks:
    val onPlayCombination: (List<Card>, Card?) -> Unit = { cards, cardToKeep ->
        // No-op for preview
    }
    val onWithdrawCards: (List<Card>) -> Unit = { cards ->
        // No-op for preview
    }

    MatView(
        playmat = playmatCards,
        discardPile = discardPile,
        selectedCards = selectedCards,
        onPlayCombination = onPlayCombination,
        onWithdrawCards = onWithdrawCards,
        cardWidth = 80.dp,   // 🚀 Example dimensions
        cardHeight = 120.dp  // 🚀 Example dimensions
    )
}


 */
