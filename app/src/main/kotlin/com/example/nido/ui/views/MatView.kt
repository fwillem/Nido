package com.example.nido.ui.views

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.example.nido.game.rules.GameRules
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
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.game.FakeGameManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import com.example.nido.game.rules.calculateTurnInfo
import com.example.nido.utils.Constants.SELECTED_CARD_OFFSET
import com.example.nido.utils.Debug
import com.example.nido.utils.sortedByMode
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*

@Composable
fun MatView(
    playmat: SnapshotStateList<Card>?,
    discardPile: SnapshotStateList<Card>,
    debug: Debug,
    currentPlayerHand: List<Card>,
    onPlayCombination: (List<Card>, Card?) -> Unit,
    onWithdrawCards: (List<Card>) -> Unit,
    onSkip: () -> Unit,
    cardWidth: Dp,
    cardHeight: Dp,
) {
    val gameManager = LocalGameManager.current
    val gameState = gameManager.gameState.value
    val turnInfo = calculateTurnInfo(gameState)


    TRACE(INFO) { "🌀 MatView recomposed" }

    val cardWidthDebug = cardWidth
    val cardHeightDebug = cardHeight

    Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(NidoColors.SelectedCardBackground)
                    .fillMaxSize()
            ) {
                if (debug.displayAIsHands) {
                val sortedCards by remember(currentPlayerHand) {
                    derivedStateOf { currentPlayerHand.sortedByMode(SortMode.COLOR) }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                ) {
                    sortedCards.forEachIndexed { index, card ->
                        val overlapOffset = (cardWidthDebug / 2) * index
                        Box(
                            modifier = Modifier
                                .offset(x = overlapOffset)
                                .zIndex(index.toFloat())
                        ) {
                            CardView(
                                card = card,
                                modifier = Modifier.size(cardWidthDebug, cardHeightDebug)
                            )
                        }
                    }
                }
                }

            }

        Box(
            modifier = Modifier
                .weight(1f)
                .background(NidoColors.PlaymatBackground)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                playmat?.let {
                    val sortedCards by remember(playmat) {
                        derivedStateOf { playmat.sortedByMode(SortMode.VALUE) }
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

            TurnActionButtons(
                turnInfo = turnInfo,
                playmat = playmat,
                onPlayCombination = onPlayCombination,
                onWithdrawCards = onWithdrawCards,
                onSkip = onSkip,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
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
    NidoTheme {
        // Provide the FakeGameManager to the CompositionLocal so that
        // every call to LocalGameManager.current returns the fake instance.
        CompositionLocalProvider(LocalGameManager provides FakeGameManager()) {
            val playmatCards = remember { mutableStateListOf(
                Card(2, "RED"),
                Card(3, "GREEN")
            )}

            val currentPLayerHand = remember { mutableStateListOf(
                Card(9, "ORANGE"),
                Card(7, "PINK")
            )}

            val selectedCards = remember { mutableStateListOf(
                Card(4, "BLUE"),
                Card(5, "MOCHA"),
                Card(6, "PINK")
            )}

            val discardPile = remember { mutableStateListOf<Card>() }

            val onPlayCombination: (List<Card>, Card?) -> Unit = { _, _ -> }
            val onWithdrawCards: (List<Card>) -> Unit = { _ -> }
            val onSkip: () -> Unit = {}

            MatView(
                playmat = playmatCards,
                discardPile = discardPile,
                currentPlayerHand = currentPLayerHand,
                debug = Debug(true,false),
                onPlayCombination = onPlayCombination,
                onWithdrawCards = onWithdrawCards,
                onSkip = onSkip,
                cardWidth = 80.dp,
                cardHeight = 120.dp,
            )
        }
    }
}
