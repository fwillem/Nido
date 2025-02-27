package com.example.nido.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.nido.utils.Constants.CARD_HEIGHT
import com.example.nido.utils.Constants.CARD_WIDTH
import kotlin.collections.get
import kotlin.invoke
import com.example.nido.data.model.Hand
import com.example.nido.data.repository.CardRepository
import com.example.nido.data.repository.CardRepository.generateDeck
import com.example.nido.data.model.Card
import com.example.nido.utils.SortMode
import com.example.nido.utils.TestData.testVectors
import com.example.nido.ui.views.ActionButtonsView
import com.example.nido.ui.views.PlayersRowView
import com.example.nido.ui.views.HandView
import com.example.nido.ui.views.MatView
import com.example.nido.game.rules.HAND_SIZE
import com.example.nido.game.GameManager
import com.example.nido.data.model.Player
import com.example.nido.game.GameViewModel


@Composable
fun DebugScreen(
    onEndGame: () -> Unit,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier) {
    var deck = CardRepository.generateDeck(shuffle = true)
    val currentPlayer by remember { derivedStateOf {  GameManager.getCurrentPlayer() } } // Get CurrentPlayer

    val currentHand = remember { Hand() }
    val playmat = remember { mutableStateListOf<Card>() }
    val discardPile = remember { mutableStateListOf<Card>() }
    val playerCounts = remember { mutableStateListOf(9, 9, 9, 9) }

    var sortMode by remember { mutableStateOf(SortMode.FIFO) }
    var testVectorIndex by remember { mutableStateOf(0) }
    val selectedCards = remember { mutableStateListOf<Card>() }

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
            ActionButtonsView(
                mapOf(
                    "New Hand" to {
                        println("New Hand clicked!")  // DEBUG
                        drawNewHand()
                    },
                    "Remove Card" to {
                        println("Remove Card clicked!")  // DEBUG
                        currentHand.removeCard()?.let { deck.add(it) }
                    },
                    "Add Card" to {
                        println("Add Card clicked!")  // DEBUG
                        deck.firstOrNull()?.let { deck.removeAt(0); currentHand.addCard(it) }
                    },
                    "Cycle Test Vector" to {
                        println("Cycle Test Vector clicked!")  // DEBUG
                        switchTestVector()
                    },
                    "Shuffle" to {
                        println("Shuffle clicked!")  // DEBUG
                        deck.shuffle()
                    },
                    "Sort Mode: ${sortMode.name}" to {
                        println("Sort Mode clicked!")  // DEBUG
                        toggleSortMode()
                    },
                    "Test Fill" to {
                        println("Test Fill clicked!")  // DEBUG
                        drawNewHand()
                        playmat.clear()
                        repeat(3) { deck.firstOrNull()?.let { deck.removeAt(0); playmat.add(it) } }
                        discardPile.clear()
                        repeat(9) { deck.firstOrNull()?.let { deck.removeAt(0); discardPile.add(it) } }
                    },
                    "Quit" to {
                        println("Quit")  // DEBUG
                        onEndGame()
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
            /*
            PlayersRowView(
                players = GameManager.players,  // ✅ Pass the full list of players
                currentTurnIndex = GameManager.currentTurnIndex // ✅ Get from GameManager
            )
            */

        }

        // Middle Section: MatView (Playmat + Discard Pile)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF228B22)),
            contentAlignment = Alignment.Center
        ) {
            MatView(playmat,
                discardPile,
                selectedCards = selectedCards ,
                onPlayCombination = { combination ->
                    if (GameManager.isValidMove(combination)) {
                        GameManager.playCombination(combination)
                    } else {
                        println("❌ Invalid move!")
                    }
                },
                cardWidth = CARD_WIDTH.dp,
                cardHeight = CARD_HEIGHT.dp)
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
                onDoubleClick = toggleSortMode,
                onSelectCard = { card ->
                    println("Selected cards: $card")
                }
            )
        }
    }
}
