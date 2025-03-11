package com.example.nido.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.data.model.Card
import com.example.nido.data.model.CardColor
import com.example.nido.data.model.Combination
import com.example.nido.game.GameManager
import com.example.nido.game.GameManager.currentPlayerHasValidCombination
import com.example.nido.game.rules.GameRules.isValidMove
import com.example.nido.events.AppEvent
import com.example.nido.ui.theme.NidoColors
import com.example.nido.ui.views.CardView
import com.example.nido.utils.Constants.NB_OF_DISCARDED_CARDS_TO_SHOW
import com.example.nido.utils.Constants.AI_THINKING_DURATION_MS
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.DEBUG
import com.example.nido.utils.TraceLogLevel.VERBOSE
import com.example.nido.utils.TraceLogLevel.INFO
import com.example.nido.utils.sortedByMode
import com.example.nido.utils.SortMode
import com.example.nido.utils.SortMode.VALUE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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

    Row(modifier = Modifier.fillMaxSize()) {
        // Left section: Selected Cards
        Box(
            modifier = Modifier
                .weight(1f)
                .background(NidoColors.SelectedCardBackground)
                .fillMaxSize()
        ) {
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
        Box(
            modifier = Modifier
                .weight(1f)
                .background(NidoColors.PlaymatBackground)
                .fillMaxSize()
        ) {
            // Playmat Row
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                playmat?.let {
                    val sortedCards by remember(playmat, VALUE) {
                        derivedStateOf { playmat.sortedByMode(VALUE) }
                    }
                    for (sortedCard in sortedCards) {
                        CardView(
                            card = sortedCard,
                            modifier = Modifier
                                .width(cardWidth)
                                .height(cardHeight)
                        )
                    }
                }
            }
            // Button section
            if (!GameManager.isCurrentPlayerLocal()) {

            }
            else if (!currentPlayerHasValidCombination()) {
                TRACE(VERBOSE) { "Local player has no valid combination" }
                var skipCount by remember { mutableStateOf(3) }
                // Use LaunchedEffect to decrement skipCount every 500ms.
                LaunchedEffect(Unit) {
                    while (skipCount > 0) {
                        delay(800L)
                        skipCount--
                    }
                    onSkip()

                }
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
                    Text("Skippy ($skipCount)", fontSize = 16.sp, lineHeight = 16.sp)
                }
            } else if (selectedCards.isNotEmpty()) {
                TRACE(DEBUG) { "Player has a valid combination" }
                val currentCombination = if (playmat != null) Combination(playmat) else Combination(mutableStateListOf())
                if (isValidMove(currentCombination, Combination(selectedCards))) {
                    Button(
                        onClick = {
                            val candidateCards = playmat?.toList() ?: emptyList()
                            when {
                                candidateCards.isEmpty() -> {
                                    TRACE(DEBUG) { "No card to keep" }
                                    onPlayCombination(selectedCards.toList(), null)
                                    selectedCards.clear()
                                }
                                candidateCards.size == 1 -> {
                                    TRACE(DEBUG) { "Only one candidate: commit immediately ${candidateCards.first()}" }
                                    onPlayCombination(selectedCards.toList(), candidateCards.first())
                                    selectedCards.clear()
                                }
                                else -> {
                                    TRACE(DEBUG) {
                                        "Several candidates: ${candidateCards.joinToString { "${it.value} ${it.color}" }}"
                                    }
                                    TRACE(INFO) { "setDialogEvent : CardSelection" }
                                    if (selectedCards.size == GameManager.getCurrentPlayerHandSize()) {
                                        onPlayCombination(selectedCards.toList(), candidateCards.first())
                                        selectedCards.clear()
                                    } else {
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
                        Text("Play", fontSize = 16.sp, lineHeight = 16.sp)
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
                        Text("Remove", fontSize = 16.sp, lineHeight = 16.sp)
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
                    Text("Skip", fontSize = 16.sp, lineHeight = 16.sp)
                }
            }
        }
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
    val onPlayCombination: (List<Card>, Card?) -> Unit = { cards, cardToKeep -> }
    val onWithdrawCards: (List<Card>) -> Unit = { cards -> }
    val onSkip: () -> Unit = { }

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
