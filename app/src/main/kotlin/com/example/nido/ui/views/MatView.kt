package com.example.nido.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.R
import com.example.nido.data.model.Card
import com.example.nido.game.BannerMsg
import com.example.nido.game.FakeGameManager
import com.example.nido.game.GameState
import com.example.nido.game.rules.calculateTurnInfo
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.theme.NidoColors
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.utils.Debug
import com.example.nido.utils.SortMode
import com.example.nido.utils.sortedByMode
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxWithConstraints

@Composable
fun MatView(
    playmat: List<Card>?,
    debug: Debug,
    currentPlayerHand: List<Card>,
    onPlayCombination: (List<Card>, Card?) -> Unit,
    onWithdrawCards: (List<Card>) -> Unit,
    onSkip: () -> Unit,
    cardWidth: Dp,
    cardHeight: Dp,
    // banner params (kept for compatibility)
    bannerWidthFraction: Float = 1f,
    bannerCornerRadius: Dp = 12.dp,
    bannerMinWidth: Dp = 104.dp,
    bannerMaxWidth: Dp = 124.dp,
    bannerMinHeight: Dp = Dp.Unspecified,
    bannerMaxHeight: Dp = 36.dp,
    bannerFontSize: TextUnit = 16.sp,
    bannerFontWeight: FontWeight = FontWeight.Bold,
    bannerBottomPadding: Dp = 8.dp,
) {
    val gameManager = LocalGameManager.current
    val gameState = gameManager.gameState.value
    val turnInfo = calculateTurnInfo(gameState)

    Row(modifier = Modifier.fillMaxSize()) {

        // Left column: reveal AI hand when debug is ON
        if (debug.displayAIsHands) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(NidoColors.SelectedCardBackground)
                    .fillMaxSize()
            ) {
                val sortedCards by remember(currentPlayerHand) {
                    derivedStateOf { currentPlayerHand.sortedByMode(SortMode.COLOR) }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        sortedCards.forEach { card ->
                            CardView(card = card, modifier = Modifier.size(cardWidth, cardHeight))
                        }
                    }
                }
            }
        }

        // Right column: playmat + banner + buttons
        Box(
            modifier = Modifier
                .weight(1f)
                .background(NidoColors.PlaymatBackground)
                .fillMaxSize()
        ) {
            // Centered playmat cards
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                playmat?.let { cards ->
                    val sorted by remember(cards) {
                        derivedStateOf { cards.sortedByMode(SortMode.VALUE) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        sorted.forEach { c ->
                            CardView(
                                card = c,
                                modifier = Modifier
                                    .width(cardWidth)
                                    .height(cardHeight)
                            )
                        }
                    }
                }
            }

            // Localized banner at the bottom (based on bannerMsg)
            val banner = matBannerText(gameState)
            banner?.let { text ->
                BoxWithConstraints(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = bannerBottomPadding)
                        .fillMaxWidth()
                ) {
                    val cardsCount = playmat?.size ?: 0
                    val fraction = bannerWidthFraction.coerceIn(0f, 1f)
                    val cardsWidth = cardWidth * cardsCount
                    var w = if (cardsCount > 0) cardsWidth * fraction else this.maxWidth * fraction
                    if (bannerMaxWidth != Dp.Unspecified && w > bannerMaxWidth) w = bannerMaxWidth
                    if (bannerMinWidth != Dp.Unspecified && w < bannerMinWidth) w = bannerMinWidth
                    if (w > this.maxWidth) w = this.maxWidth

                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(w)
                            .heightIn(min = bannerMinHeight, max = bannerMaxHeight),
                        containerColor = NidoColors.HandViewBackground.copy(alpha = 0.6f),
                        contentColor = androidx.compose.ui.graphics.Color.White,
                        shape = RoundedCornerShape(bannerCornerRadius)
                    ) {
                        Text(
                            text,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = bannerFontWeight,
                            fontSize = bannerFontSize,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Turn action buttons (positioned on the right)
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

@Composable
private fun matBannerText(state: GameState): String? {
    return when (val b = state.bannerMsg) {
        is BannerMsg.Play -> stringResource(R.string.banner_play, b.name)
        null              -> null
    }
}

// -------- Preview (kept as before) --------

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
            val playmatCards: SnapshotStateList<Card> = remember {
                mutableStateListOf(
                    Card(2, "RED"),
                    Card(3, "GREEN")
                )
            }
            val currentPlayerHand: SnapshotStateList<Card> = remember {
                mutableStateListOf(
                    Card(9, "ORANGE"),
                    Card(7, "PINK"),
                    Card(6, "BLUE")
                )
            }

            val onPlayCombination: (List<Card>, Card?) -> Unit = { _, _ -> }
            val onWithdrawCards: (List<Card>) -> Unit = { _ -> }
            val onSkip: () -> Unit = {}

            MatView(
                playmat = playmatCards,
                currentPlayerHand = currentPlayerHand,
                debug = Debug(displayAIsHands = true, doNotAutoPlayerAI = false),
                onPlayCombination = onPlayCombination,
                onWithdrawCards = onWithdrawCards,
                onSkip = onSkip,
                cardWidth = 80.dp,
                cardHeight = 120.dp
            )
        }
    }
}
