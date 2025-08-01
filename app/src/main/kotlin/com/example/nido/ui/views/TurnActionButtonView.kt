package com.example.nido.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.data.model.Card
import com.example.nido.events.AppEvent
import com.example.nido.game.TurnInfo
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.theme.NidoColors
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.DEBUG
import com.example.nido.utils.TraceLogLevel.INFO
import kotlinx.coroutines.delay
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.res.stringResource
import com.example.nido.R

@Composable
fun TurnActionButtons(
    turnInfo: TurnInfo,
    playmat: SnapshotStateList<Card>?,
    onPlayCombination: (List<Card>, Card?) -> Unit,
    onWithdrawCards: (List<Card>) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    require(
        listOf(turnInfo.displaySkipCounter, turnInfo.displaySkip, turnInfo.displayPlay).count { it } <= 1
    ) { "Only one of displaySkipCounter, displaySkip, displayPlay should be true!" }

    val gameState = LocalGameManager.current.gameState.value
    val currentPlayer = gameState.players[gameState.currentPlayerIndex]
    val selectedCards = currentPlayer.hand.cards.filter { it.isSelected }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (turnInfo.displaySkipCounter) {
            SkipButtonWithTimer(onSkip)
        } else if (turnInfo.displaySkip) {
            SkipButton(onSkip)
        } else if (turnInfo.displayPlay) {
            PlayButton(
                playmat = playmat,
                selectedCards = selectedCards,
                onPlayCombination = onPlayCombination
            )
        }
        if (turnInfo.displayRemove) {
            RemoveButton(
                selectedCards = selectedCards,
                onWithdrawCards = onWithdrawCards
            )
        }
    }
}

@Composable
private fun SkipButton(onSkip: () -> Unit) {
    Button(
        onClick = onSkip,
        colors = ButtonDefaults.buttonColors(
            containerColor = NidoColors.PlayMatButtonBackground.copy(alpha = 0.8f),
            contentColor = Color.White
        ),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(stringResource(R.string.skip), fontSize = 16.sp)
    }
}

@Composable
private fun SkipButtonWithTimer(onSkip: () -> Unit) {
    var skipTimerCount by remember { mutableStateOf(5) }
    LaunchedEffect(Unit) {
        while (skipTimerCount > 0) {
            delay(800L)
            skipTimerCount--
        }
        onSkip()
    }
    Button(
        onClick = onSkip,
        colors = ButtonDefaults.buttonColors(
            containerColor = NidoColors.PlayMatButtonBackground.copy(alpha = 0.8f),
            contentColor = Color.White
        ),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text("${stringResource(R.string.skip)} ($skipTimerCount)", fontSize = 16.sp)
    }
}

@Composable
private fun PlayButton(
    playmat: SnapshotStateList<Card>?,
    selectedCards: List<Card>,
    onPlayCombination: (List<Card>, Card?) -> Unit,
) {
    val gameManager = LocalGameManager.current

    Button(
        onClick = {
            val candidateCards = playmat?.toList() ?: emptyList()
            when {
                candidateCards.isEmpty() -> {
                    onPlayCombination(selectedCards, null)
                    selectedCards.forEach { it.isSelected = false }
                }

                candidateCards.size == 1 -> {
                    onPlayCombination(selectedCards, candidateCards.first())
                    selectedCards.forEach { it.isSelected = false }
                }

                else -> {
                    TRACE(DEBUG) {
                        "Several candidates: ${candidateCards.joinToString { "${it.value} ${it.color}" }}"
                    }
                    TRACE(INFO) { "setDialogEvent : CardSelection" }

                    if (gameManager.hasPlayedAllRemainingCards()) {
                        // The player won
                        onPlayCombination(selectedCards, candidateCards.first())
                        selectedCards.forEach { it.isSelected = false }
                    } else {
                        gameManager.setDialogEvent(
                            AppEvent.GameEvent.CardSelection(
                                candidateCards = candidateCards,
                                selectedCards = selectedCards,
                                onConfirm = { chosenCard ->
                                    onPlayCombination(selectedCards, chosenCard)
                                    selectedCards.forEach { it.isSelected = false }
                                    gameManager.clearDialogEvent()
                                },
                                onCancel = {
                                    gameManager.clearDialogEvent()
                                }
                            )
                        )
                    }
                }
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = NidoColors.PlayMatButtonBackground.copy(alpha = 0.8f),
            contentColor = Color.White
        ),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(stringResource(R.string.play), fontSize = 16.sp)
    }
}

@Composable
private fun RemoveButton(
    selectedCards: List<Card>,
    onWithdrawCards: (List<Card>) -> Unit
) {
    Button(
        onClick = {
            selectedCards.forEach { it.isSelected = false }
            onWithdrawCards(selectedCards)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = NidoColors.PlayMatButtonBackground.copy(alpha = 0.8f),
            contentColor = Color.White
        ),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(stringResource(R.string.remove), fontSize = 16.sp)
    }
}
