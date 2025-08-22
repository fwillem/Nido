package com.example.nido.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.data.model.Card
import com.example.nido.ui.theme.NidoColors
import com.example.nido.utils.Debug
import com.example.nido.utils.SortMode
import com.example.nido.utils.sortedByMode

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
    // paramètres de banner (non utilisés ici mais fournis pour compat)
    bannerWidthFraction: Float = 1f,
    bannerCornerRadius: Dp = 12.dp,
    bannerMinWidth: Dp = 104.dp,
    bannerMaxWidth: Dp = 124.dp,
    bannerMinHeight: Dp = Dp.Unspecified,
    bannerMaxHeight: Dp = 36.dp,
    bannerFontSize: TextUnit = 16.sp,
    bannerFontWeight: androidx.compose.ui.text.font.FontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
    bannerBottomPadding: Dp = 8.dp,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        if (debug.displayAIsHands) {
            // Colonne gauche: main du joueur en clair (triée) quand debug actif
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
                    // Affichage simple: largeur fixe par carte
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        sortedCards.forEach { card ->
                            CardView(card = card, modifier = Modifier.size(cardWidth, cardHeight))
                        }
                    }
                }
            }
        }

        // Colonne droite: tapis
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
            val playmatCards = remember {
                mutableStateListOf(
                    Card(2, "RED"),
                    Card(3, "GREEN")
                )
            }

            val currentPLayerHand = remember {
                mutableStateListOf(
                    Card(9, "ORANGE"),
                    Card(7, "PINK")
                )
            }


            val onPlayCombination: (List<Card>, Card?) -> Unit = { _, _ -> }
            val onWithdrawCards: (List<Card>) -> Unit = { _ -> }
            val onSkip: () -> Unit = {}

            MatView(
                playmat = playmatCards,
                currentPlayerHand = currentPLayerHand,
                debug = Debug(true, false),
                onPlayCombination = onPlayCombination,
                onWithdrawCards = onWithdrawCards,
                onSkip = onSkip,
                cardWidth = 80.dp,
                cardHeight = 120.dp,
            )
        }
    }
}
