package com.example.nido.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.data.model.Card
import com.example.nido.game.GameManager
import com.example.nido.ui.views.ActionButtonsView
import com.example.nido.ui.views.HandView
import com.example.nido.ui.views.MatView
import com.example.nido.utils.Constants.CARD_HEIGHT
import com.example.nido.utils.Constants.CARD_WIDTH
import com.example.nido.utils.SortMode
import com.example.nido.game.GameViewModel
import com.example.nido.ui.views.PlayersRowView

@Composable
fun MainScreen(
    onEndGame: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel  // Add viewModel parameter
) {


    val gameState by viewModel.gameManager.gameState // Observe the ENTIRE GameState

    LaunchedEffect(gameState) {
        println("MainScreen: gameState.players=${gameState.players}")
    }


    // Use derivedStateOf for values that depend on gameState
    val currentPlayer by remember {
        derivedStateOf {
            require(gameState.players.isNotEmpty()) { "GameState.players is empty – this should not happen!" }
            gameState.players[gameState.currentPlayerIndex]
        }
    }


    val currentHand by remember { derivedStateOf { currentPlayer.hand.cards } }
    val playmatCards by remember { derivedStateOf { gameState.currentCombinationOnMat?.cards ?: emptyList() } }
    val discardPile by remember { derivedStateOf { gameState.discardPile } }
    val players by remember { derivedStateOf { gameState.players } }
    val currentTurnIndex by remember { derivedStateOf { gameState.currentPlayerIndex }}
    val playmat by remember { derivedStateOf { gameState.currentCombinationOnMat }}


    var sortMode by remember { mutableStateOf(SortMode.FIFO) }
    val toggleSortMode: () -> Unit = {
        sortMode = when (sortMode) {
            SortMode.FIFO -> SortMode.COLOR
            SortMode.COLOR -> SortMode.VALUE
            SortMode.VALUE -> SortMode.FIFO
        }
    }

    val selectedCards = remember { mutableStateListOf<Card>() }

    val isValidMove = remember(selectedCards) {
        GameManager.isValidMove(selectedCards)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF006400)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 🔹 Top Row: Action Buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {

            // 🔹 Action Buttons (Manual AI Move)
            ActionButtonsView(
                mapOf(
                    "Sort Mode: ${sortMode.name}" to { toggleSortMode() },
                    "Quit" to { onEndGame() },
                    "Play AI Move" to { GameManager.processAIMove() }
                )
            )

        }

        // 🔹 Player Information Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFF004000)),
            contentAlignment = Alignment.Center
        ) {
            PlayersRowView(
                players = players,
                currentTurnIndex = currentTurnIndex
            )
        }


        // 🔹 Middle Section: MatView (Playmat + Discard Pile)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF228B22)),
            contentAlignment = Alignment.Center
        ) {

            val playmatSnapshotList = playmat?.cards?.let { cardList ->
                mutableStateListOf<com.example.nido.data.model.Card>().apply {
                    addAll(cardList)
                }
            } ?: mutableStateListOf()


            MatView(
                playmat = playmatSnapshotList,
                discardPile = discardPile,
                selectedCards = selectedCards,
                onPlayCombination = { playedCards ->  // ✅ Use a different name
                    if (GameManager.isValidMove(playedCards)) {
                        println("✅ Move is valid! Playing: $playedCards")
                        GameManager.playCombination(playedCards)
                        selectedCards.clear() // ✅ This now correctly refers to the outer selectedCards
                    } else {
                        println("❌ Invalid move!")
                    }
                },



                cardWidth = CARD_WIDTH.dp,
                cardHeight = CARD_HEIGHT.dp
            )
        }




        // 🔹 Bottom Section: HandView (Player's Hand)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF006400)),
            contentAlignment = Alignment.Center
        ) {
            HandView(
                hand = com.example.nido.data.model.Hand(
                    mutableStateListOf<Card>().apply { addAll(currentHand) } // ✅ Convert List to SnapshotStateList
                ),
                cardWidth = CARD_WIDTH.dp,
                cardHeight = CARD_HEIGHT.dp,
                sortMode = sortMode,
                onDoubleClick = toggleSortMode,
                onSelectCard = { card ->
                    if (selectedCards.contains(card)) {
                        selectedCards.remove(card)
                    } else {
                        selectedCards.add(card)
                    }
                    println("selectedCards: $selectedCards")
                }
            )

        }
    }
}