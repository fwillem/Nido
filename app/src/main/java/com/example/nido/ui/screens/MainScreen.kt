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
import com.example.nido.utils.Constants
import com.example.nido.utils.SortMode
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.example.nido.data.model.PlayerType
import com.example.nido.events.AppEvent
import com.example.nido.utils.Constants.AI_THINKING_DURATION_MS
import com.example.nido.ui.dialogs.*

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview
import com.example.nido.data.model.CardColor
import com.example.nido.data.model.Hand
import com.example.nido.data.model.Player
import com.example.nido.data.model.Combination
import com.example.nido.data.model.PlayerAction
import com.example.nido.data.model.PlayerActionType
import com.example.nido.game.GameState
import com.example.nido.game.GameScreens
import com.example.nido.ui.screens.MainScreen
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.ui.theme.NidoColors

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
        modifier = Modifier
            .fillMaxSize(),

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
                .height( Constants.PLAYERS_ROW_HEIGHT.dp)
                .background(NidoColors.PlayersRowBackground),

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
                .weight(1.1f)
                .background(NidoColors.MatViewBackground),
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
                onSkip = {GameManager.processSkip()},
                cardWidth = Constants.CARD_ON_MAT_WIDTH.dp,
                cardHeight = Constants.CARD_ON_MAT_HEIGHT.dp
            )
        }

        // 🔹 Bottom Section: HandView (Player's Hand)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(NidoColors.HandViewBackground),
            contentAlignment = Alignment.Center
        ) {
            HandView(
                hand = com.example.nido.data.model.Hand(
                    mutableStateListOf<Card>().apply { addAll(currentHand) }
                ),
                cardWidth = Constants.CARD_ON_HAND_WIDTH.dp,
                cardHeight = Constants.CARD_ON_HAND_HEIGHT.dp,
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
    // 🚀 In MainScreen.kt, inside your composable:
    if (gameState.gameEvent != null) { // Check if any event exists
        when (val event = gameState.gameEvent) { // Branch by event type
            is AppEvent.GameEvent.CardSelection -> {
                CardSelectionDialog(event = event)
            }
            is AppEvent.GameEvent.RoundOver -> {
                RoundOverDialog(event = event)
            }
            is AppEvent.GameEvent.GameOver -> {
                GameOverDialog(event = event)
            }
            is AppEvent.PlayerEvent.PlayerLeft -> {
                PlayerLeftDialog(event = event)
            }
            is AppEvent.PlayerEvent.ChatMessage -> {
                ChatMessageDialog(event = event)
            }
            else -> {
                TRACE(FATAL) { "Unknown event type: ${gameState.gameEvent}" }
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


// 🚀 SimplePlayer: A basic implementation of the Player interface for preview purposes.
data class SimplePlayer(
    override val id: String,
    override val name: String,
    override val avatar: String = "",
    override val playerType: PlayerType,
    override var score: Int = 0,
    override val hand: Hand
) : Player {
    override fun play(gameManager: com.example.nido.game.GameManager): PlayerAction {
        // Return a dummy action (SKIP) for preview purposes.
        return PlayerAction(PlayerActionType.SKIP)
    }
    override fun copy(
        id: String,
        name: String,
        avatar: String,
        score: Int,
        hand: Hand
    ): Player {
        return SimplePlayer(id, name, avatar, playerType, score, hand)
    }
}


@Preview(
    name = "Landscape MainScreen Preview",
    widthDp = 800, // 🚀 wider than it is tall
    heightDp = 400, // 🚀 adjust as needed
    showBackground = true
)
//@Preview(showBackground = true, name = "MainScreen Preview")
@Composable
fun PreviewMainScreen() {
    // 🚀 Create dummy players using SimplePlayer.
    val dummyPlayers = listOf(
        SimplePlayer(
            id = "1",
            name = "Alice",
            playerType = PlayerType.LOCAL,
            hand = Hand(mutableStateListOf(
                // 9 cards for the current player's hand
                Card(2, "RED"),
                Card(3, "RED"),
                Card(4, "GREEN"),
                Card(3, "MOCHA"),
                Card(3, "PINK"),
                Card(3, "GREEN"),
                Card(2, "BLUE"),
                Card(5, "ORANGE"),
                Card(4, "RED")
            ))
        ),
        SimplePlayer(
            id = "2",
            name = "Bob",
            playerType = PlayerType.AI,
            hand = Hand(mutableStateListOf(
                Card(2, "RED"),
                Card(3, "GREEN"),
                Card(4, "BLUE")
            ))
        ),
        SimplePlayer(
            id = "3",
            name = "Carol",
            playerType = PlayerType.LOCAL,
            hand = Hand(mutableStateListOf(
                Card(2, "PINK"),
                Card(3, "MOCHA"),
                Card(4, "GREEN")
            ))
        )
    )

    // 🚀 Create a dummy playmat with a couple of cards.
    val dummyPlaymat = Combination(mutableStateListOf(
        Card(3, "RED"),
        Card(3, "MOCHA")
    ))

    // 🚀 Create dummy selected cards (for example, 2 cards).
    val dummySelectedCards = mutableStateListOf<Card>().apply {
        addAll(listOf(
            Card(4, "GREEN"),
            Card(5, "PINK")
        ))
    }

    // 🚀 Create a dummy discard pile with a couple of cards.
    val dummyDiscardPile = mutableStateListOf<Card>().apply {
        addAll(listOf(
            Card(2, "BLUE"),
            Card(3, "ORANGE")
        ))
    }

    // 🚀 Create a dummy deck with a few cards.
    val dummyDeck = mutableStateListOf<Card>().apply {
        addAll(listOf(
            Card(2, "RED"),
            Card(3, "RED"),
            Card(4, "RED")
        ))
    }

    // 🚀 Build a dummy game state.
    val dummyGameState = GameState(
        screen = GameScreens.PLAYING,
        pointLimit = 100,
        players = dummyPlayers,
        startingPlayerIndex = 0,
        currentPlayerIndex = 0,
        currentCombinationOnMat = dummyPlaymat,
        discardPile = dummyDiscardPile,
        selectedCards = dummySelectedCards,
        deck = dummyDeck,
        skipCount = 0,
        soundOn = true,
        showConfirmExitDialog = false,
        gameEvent = null,
        turnId = 1
    )

    // 🚀 Create a fake GameViewModel by instantiating it normally and applying our dummy state.
    val fakeViewModel = GameViewModel().apply {  // 🚀 Using apply() to update state
        updateGameState(dummyGameState)
    }


    NidoTheme {
        MainScreen(
            onEndGame = {},
            viewModel = fakeViewModel
        )
    }
}