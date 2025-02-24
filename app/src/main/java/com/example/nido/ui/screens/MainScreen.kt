package com.example.nido.ui.screens

import androidx.compose.ui.tooling.preview.Preview
import com.example.nido.ui.theme.NidoTheme

package com.example.nido

import androidx.compose.foundation.Image
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
import androidx.compose.material3.value
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var deck by remember { mutableStateOf(generateDeck(shuffle = true)) }
    val currentHand = remember { Hand() }
    val playmat = remember { mutableStateListOf<Card>() }
    val discardPile = remember { mutableStateListOf<Card>() }
    val playerCounts = remember { mutableStateListOf(9, 9, 9, 9) }

    var sortMode by remember { mutableStateOf(SortMode.FIFO) }
    var testVectorIndex by remember { mutableStateOf(0) }

    val switchTestVector: () -> Unit = {
        testVectorIndex = (testVectorIndex + 1) % testVectors.size
        currentHand.clear()
        testVectors[testVectorIndex]().cards.forEach { currentHand.addCard(it) }
    }

    val toggleSortMode: () -> Unit = {
        sortMode = when (sortMode) {
            SortMode.FIFO -> SortMode.COLOR
            SortMode.COLOR -> SortMode.VALUE
            SortMode.VALUE -> SortMode.FIFO
        }
    }

    val drawNewHand: () -> Unit = {
        currentHand.clear()
        playmat.clear()
        discardPile.clear()
        val cardsToTake = minOf(HAND_SIZE, deck.size)
        repeat(cardsToTake) { currentHand.addCard(deck.removeAt(0)) }
        if (deck.isEmpty()) deck = generateDeck(shuffle = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF006400)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: Action Buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            ActionButtonsRow(
                mapOf(
                    "New Hand" to drawNewHand,
                    "Remove Card" to { currentHand.removeCard()?.let { deck.add(it) } },
                    "Add Card" to { deck.firstOrNull()?.let { deck.removeAt(0); currentHand.addCard(it) } },
                    "Cycle Test Vector" to switchTestVector,
                    "Shuffle" to { deck.shuffle() },
                    "Sort Mode: ${sortMode.name}" to toggleSortMode,
                    "Test Fill" to {
                        drawNewHand()
                        playmat.clear()
                        repeat(3) { deck.firstOrNull()?.let { deck.removeAt(0); playmat.add(it) } }
                        discardPile.clear()
                        repeat(9) { deck.firstOrNull()?.let { deck.removeAt(0); discardPile.add(it) } }
                    }
                )
            )
        }

        // Player Information Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFF004000)),
            contentAlignment = Alignment.Center
        ) {
            PlayerRow(playerCounts = playerCounts, currentPlayerIndex = 1)
        }

        // Middle Section: MatView (Playmat + Discard Pile)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF228B22)),
            contentAlignment = Alignment.Center
        ) {
            // matView(playmat, discardPile)
        }

        // Bottom Section: HandView (Player's Hand)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF006400)),
            contentAlignment = Alignment.Center
        ) {
            HandView(
                hand = currentHand,
                cardWidth = CARD_WIDTH.dp,
                cardHeight = CARD_HEIGHT.dp,
                sortMode = sortMode,
                onDoubleClick = toggleSortMode
            )
        }
    }
}
