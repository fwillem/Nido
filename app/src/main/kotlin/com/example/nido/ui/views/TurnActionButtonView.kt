package com.example.nido.ui.views

import android.graphics.Color.alpha
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import android.media.MediaPlayer
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.*
import com.example.nido.events.GameDialogEvent
import com.example.nido.utils.TraceLogLevel

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

       // AIPlayButton()
        if (turnInfo.displayManualAIPlay) {
            AIPlayButton()
        }

        // Remote Ping Button
        if (turnInfo.displayNotifyRemotePlayer) {
            NotifyRemotePlayerButton()
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

            // TODO TOREMOVE JUST FOR TESTING BSOD
            /*
            TRACE (TraceLogLevel.FATAL) {
                "PlayButton clicked with selectedCards: ${selectedCards.joinToString { "${it.value} ${it.color}" }}"
            }
             */

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
                        gameManager.setGameDialogEvent(
                            GameDialogEvent.CardSelection(
                                candidateCards = candidateCards,
                                selectedCards = selectedCards,
                                onConfirm = { chosenCard ->
                                    onPlayCombination(selectedCards, chosenCard)
                                    selectedCards.forEach { it.isSelected = false }
                                    gameManager.clearGameDialogEvent()
                                },
                                onCancel = {
                                    gameManager.clearGameDialogEvent()
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

@Composable
private fun AIPlayButton() {
    val gameManager = LocalGameManager.current

    Button(
        onClick = {
            gameManager.getAIMove()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = NidoColors.PlayMatButtonBackground.copy(alpha = 0.8f),
            contentColor = Color.White
        ),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(stringResource(R.string.playai), fontSize = 16.sp)
    }
}


@Composable
private fun NotifyRemotePlayerButton() {
    val context = LocalContext.current
    val mediaPlayer = remember {
        MediaPlayer.create(context, R.raw.siren).apply {
            isLooping = true // la sirène tourne en boucle
        }
    }

    // Lance la sirène dès que le composable apparaît
    LaunchedEffect(Unit) {
        mediaPlayer.start()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pingBlink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pingAlpha"
    )

    Button(
        onClick = {
            mediaPlayer.stop() // stoppe la sirène au clic
            mediaPlayer.release() // libère les ressources
            // Do Some Network Stuff
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = NidoColors.PlayMatButtonBackground.copy(alpha = 0.8f),
            contentColor = Color.White
        ),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.ping), // "Ping 🏓"
            fontSize = 16.sp,
            color = Color.Red.copy(alpha = alpha)
        )
    }
}
