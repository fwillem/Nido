package com.example.nido.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import com.example.nido.game.GameManager
import com.example.nido.game.GameViewModel
import com.example.nido.ui.views.ActionButtonsView
import com.example.nido.ui.views.HandView
import com.example.nido.ui.views.MatView
import com.example.nido.ui.views.PlayersRowView
import com.example.nido.utils.Constants.CARD_HEIGHT
import com.example.nido.utils.Constants.CARD_WIDTH
import com.example.nido.utils.SortMode
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.example.nido.data.model.PlayerType
import com.example.nido.events.AppEvent
import com.example.nido.utils.Constants.AI_THINKING_DURATION_MS

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
    val selectedCards = gameState.selectedCards

    var sortMode by remember { mutableStateOf(SortMode.FIFO) }
    val toggleSortMode: () -> Unit = {
        sortMode = when (sortMode) {
            SortMode.FIFO -> SortMode.COLOR
            SortMode.COLOR -> SortMode.VALUE
            SortMode.VALUE -> SortMode.FIFO
        }
    }

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
                currentTurnIndex = currentTurnIndex,
                turnID = gameState.turnId
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
                mutableStateListOf<Card>().apply { addAll(cardList) }
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
                        TRACE(FATAL, tag = "MatView:onPlayCombination") { "❌ Invalid move!" }
                    }
                },
                onWithdrawCards = { cardsToWithdraw ->
                    TRACE(DEBUG) { "Withdraw Cards: $cardsToWithdraw" }
                    // Remove cards from selected cards
                    selectedCards.removeAll(cardsToWithdraw)
                    // Trick to force a UI refresh on the HandView
                    val updatedHand = currentPlayer.hand.copy()
                    currentPlayer.hand.cards.clear()
                    currentPlayer.hand.cards.addAll(updatedHand.cards)
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

    // ── Centralized Dialog Observer ── Added this block to observe gameEvent and display AlertDialog
    // Centralized Event Observer: Monitor all AppEvent types
    if (gameState.gameEvent != null) { // Check if any event exists
        when (val event = gameState.gameEvent) { // Use a when-expression to branch by event type

            is AppEvent.GameEvent.CardSelection -> { // Handle CardSelection event
                AlertDialog(
                    onDismissRequest = { event.onCancel() },
                    title = { Text("Select Card to Keep") },
                    text = {
                        Column {
                            event.candidateCards.forEach { card ->
                                Button(
                                    onClick = { event.onConfirm(card) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = card.color.uiColor.copy(alpha = 0.6f),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Text("${card.value} ${card.color}", fontSize = 12.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { event.onCancel() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.Gray
                            ),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                       }
                    },
                    containerColor = Color.Transparent

//                    containerColor = Color.White.copy(alpha = 0.7f)
                )
            }

            is AppEvent.GameEvent.RoundOver -> {
                AlertDialog(
                    onDismissRequest = { GameManager.clearDialogEvent() },
                    title = {
                        Card(
                            modifier = Modifier.background(Color.White.copy(alpha = 0.7f))
                        ) {
                            Text("Round Over")
                        }
                    },
                    text = {
                        Card(modifier = Modifier.background(Color.White)) {
                            Text(
                                "Winner: ${event.winner.name}\n" +
                                        "Old Score: ${event.oldScore}\n" +
                                        "Points Added: ${event.pointsAdded}\n" +
                                        "New Score: ${event.newScore}"
                            )
                        }
                    },

                    confirmButton = {
                        Button(
                            onClick = { GameManager.clearDialogEvent() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.Gray
                            )
                        ) {
                            Text("OK")
                        }
                    },
                    containerColor = Color.White.copy(alpha = 0.7f)
                )
            }

            is AppEvent.GameEvent.GameOver -> {
                AlertDialog(
                    onDismissRequest = { GameManager.clearDialogEvent() },
                    title = { Text("Game Over") },
                    text = {
                        Text("Winner: ${event.playerRankings.first()}")
                    },
                    confirmButton = {
                        Button(
                            onClick = { GameManager.clearDialogEvent() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.Gray
                            )
                        ) {
                            Text("OK")
                        }
                    },
                    containerColor = Color.White.copy(alpha = 0.7f)
                )
            }

            is AppEvent.PlayerEvent.PlayerLeft -> {
                AlertDialog(
                    onDismissRequest = { GameManager.clearDialogEvent() },
                    title = { Text("Player Left") },
                    text = {
                        Text("${event.player.name} has left the game.")
                    },
                    confirmButton = {
                        Button(
                            onClick = { GameManager.clearDialogEvent() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.Gray
                            )
                        ) {
                            Text("OK")
                        }
                    },
                    containerColor = Color.White.copy(alpha = 0.7f)
                )
            }

            is AppEvent.PlayerEvent.ChatMessage -> { // 🚀 Handle ChatMessage event
                AlertDialog(
                    onDismissRequest = { GameManager.clearDialogEvent() }, // 🚀
                    title = { Text("New Chat Message") }, // 🚀
                    text = {
                        Text("${event.sender.name}: ${event.message}") // 🚀
                    },
                    confirmButton = {
                        Button(
                            onClick = { GameManager.clearDialogEvent() }, // 🚀
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.Gray
                            )
                        ) {
                            Text("OK")
                        }
                    },
                    containerColor = Color.White.copy(alpha = 0.7f) // 🚀
                )
            }
            else -> {
                TRACE(FATAL) { "Unknown AppEvent type: $event" }
            }

        }
    }

    LaunchedEffect(gameState.turnId) {
        if (currentPlayer.playerType == PlayerType.AI) {
            TRACE(INFO) { "AI will play in ${AI_THINKING_DURATION_MS / 1000} seconds..." }
            // Delay for n milliseconds (e.g., 2000 ms = 2 seconds)
            kotlinx.coroutines.delay(AI_THINKING_DURATION_MS)
            GameManager.processAIMove()
        }
    }


}
