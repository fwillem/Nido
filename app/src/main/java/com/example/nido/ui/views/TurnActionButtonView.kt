package com.example.nido.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.game.TurnInfo
import com.example.nido.ui.theme.NidoColors
import kotlinx.coroutines.delay
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.nido.data.model.Card
@Composable
fun TurnActionButtons(
    turnInfo: TurnInfo,
    playmat: SnapshotStateList<Card>?,
    selectedCards: SnapshotStateList<Card>,
    onPlayCombination: (List<Card>, Card?) -> Unit,
    onWithdrawCards: (List<Card>) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier

) {
    // Assert only one main action button is visible
    require(
        listOf(turnInfo.displaySkipCounter, turnInfo.displaySkip, turnInfo.displayPlay).count { it } <= 1
    ) { "Only one of displaySkipCounter, displaySkip, displayPlay should be true!" }



    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Skip with counter (timer)
        if (turnInfo.displaySkipCounter) {
            SkipButtonWithTimer(onSkip)
        } else if (turnInfo.displaySkip) {
            SkipButton(onSkip)
        }
        else if (turnInfo.displayPlay) {
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
        Text("Skip", fontSize = 16.sp)
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
        Text("Skip ($skipTimerCount)", fontSize = 16.sp)
    }
}

@Composable
private fun PlayButton(
    playmat: SnapshotStateList<Card>?,
    selectedCards: SnapshotStateList<Card>,
    onPlayCombination: (List<Card>, Card?) -> Unit
) {
    Button(
        onClick = {
            val candidateCards = playmat?.toList() ?: emptyList()
            when {
                candidateCards.isEmpty() -> {
                    onPlayCombination(selectedCards.toList(), null)
                    selectedCards.clear()
                }
                candidateCards.size == 1 -> {
                    onPlayCombination(selectedCards.toList(), candidateCards.first())
                    selectedCards.clear()
                }
                else -> {
                    // Add dialog logic if needed
                }
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = NidoColors.PlayMatButtonBackground.copy(alpha = 0.8f),
            contentColor = Color.White
        ),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text("Play", fontSize = 16.sp)
    }
}

@Composable
private fun RemoveButton(
    selectedCards: SnapshotStateList<Card>,
    onWithdrawCards: (List<Card>) -> Unit
) {
    Button(
        onClick = { onWithdrawCards(selectedCards.toList()) },
        colors = ButtonDefaults.buttonColors(
            containerColor = NidoColors.PlayMatButtonBackground.copy(alpha = 0.8f),
            contentColor = Color.White
        ),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text("Remove", fontSize = 16.sp)
    }
}
