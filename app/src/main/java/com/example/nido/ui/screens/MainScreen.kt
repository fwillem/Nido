package com.example.nido.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.example.nido.data.model.PlayerType


@Composable
fun MainScreen(
    onEndGame: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel  // Add viewModel parameter
) {
    val gameState by viewModel.gameState

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
    val currentTurnIndex by remember { derivedStateOf { gameState.currentPlayerIndex } }
    val playmat by remember { derivedStateOf { gameState.currentCombinationOnMat } }

    var sortMode by remember { mutableStateOf(SortMode.FIFO) }
    val toggleSortMode: () -> Unit = {
        sortMode = when (sortMode) {
            SortMode.FIFO -> SortMode.COLOR
            SortMode.COLOR -> SortMode.VALUE
            SortMode.VALUE -> SortMode.FIFO
        }
    }

    val selectedCards = remember { mutableStateListOf<Card>() }

    TRACE(VERBOSE) { "Recomposing MainScreen : current player is ${currentPlayer.name}" }

    // Dynamically build the action buttons map based on the current player's type.
    val actionButtonsMap: Map<String, () -> Unit> = if (currentPlayer.playerType == PlayerType.LOCAL) {
        mapOf(
            "Sort Mode: ${sortMode.name}" to { toggleSortMode() },
            "Quit" to { onEndGame() },
            "Skip" to { GameManager.processSkip() }  // Process skip for local players.
        )
    } else {
        mapOf(
            "Sort Mode: ${sortMode.name}" to { toggleSortMode() },
            "Quit" to { onEndGame() },
            "Play AI Move" to { GameManager.processAIMove() }
        )
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
            ActionButtonsView(actionButtonsMap)
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
                mutableStateListOf<Card>().apply {
                    addAll(cardList)
                }
            } ?: mutableStateListOf()

            MatView(
                playmat = playmatSnapshotList,
                discardPile = discardPile,
                selectedCards = selectedCards,
                onPlayCombination = { playedCards, cardToKeep ->  // ✅ Use a different name
                    if (GameManager.isValidMove(playedCards)) {
                        TRACE(DEBUG, tag = "MatView:onPlayCombination") { "✅ Move is valid! Playing: $playedCards" }
                        GameManager.playCombination(playedCards, cardToKeep)
                        selectedCards.clear() // Clear selection after a valid move.
                    } else {
                        TRACE(ERROR, tag = "MatView:onPlayCombination") { "❌ Invalid move!" }
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
                    mutableStateListOf<Card>().apply { addAll(currentHand) }
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
                    TRACE(DEBUG, tag = "HandView:onSelectCard") { "Selected Cards: $selectedCards" }
                }
            )
        }
    }
}
