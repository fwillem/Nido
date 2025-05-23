package com.example.nido.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nido.data.SavedPlayer
import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Hand
import com.example.nido.data.model.PlayerType
import com.example.nido.events.AppEvent
import com.example.nido.game.FakeGameManager
import com.example.nido.game.GameState
import com.example.nido.game.LocalPlayer
import com.example.nido.game.TurnInfo
import com.example.nido.game.ai.AIPlayer
import com.example.nido.game.IGameViewModelPreview
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.dialogs.*
import com.example.nido.ui.theme.NidoColors
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.ui.views.CommentsView
import com.example.nido.ui.views.HandView
import com.example.nido.ui.views.MatView
import com.example.nido.ui.views.PlayersRowView
import com.example.nido.utils.Constants
import com.example.nido.utils.Constants.AI_THINKING_DURATION_MS
import com.example.nido.utils.Constants.GAME_DEFAULT_POINT_LIMIT
import com.example.nido.utils.SortMode
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*

@Composable
fun MainScreen(
    onEndGame: () -> Unit,
    onQuitGame: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IGameViewModelPreview
) {
    val gameManager = LocalGameManager.current  // ✅ Retrieve injected GameManager
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

    var sortMode by remember { mutableStateOf(SortMode.COLOR) }
    val toggleSortMode: () -> Unit = {
        sortMode = when (sortMode) {
            SortMode.FIFO -> SortMode.COLOR
            SortMode.COLOR -> SortMode.VALUE
            SortMode.VALUE -> SortMode.COLOR
        }
    }

    TRACE(VERBOSE) { "Recomposing MainScreen : current player is ${currentPlayer.name}" }

    val actionButtonsMap: Map<String, () -> Unit> =
        mapOf(
            "Sort Mode: ${sortMode.name}" to { toggleSortMode() },
            "Quit" to {
                gameManager.setDialogEvent(AppEvent.GameEvent.QuitGame)
            },
        )

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
            CommentsView(actionButtonsMap)
        }

        // 🔹 Player Information Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Constants.PLAYERS_ROW_HEIGHT.dp)
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
                playerHandSize = gameManager.getCurrentPlayerHandSize(),
                onPlayCombination = { playedCards, cardToKeep ->
                    println("PNB On Play Combination currentHand(${currentHand.size}) = $currentHand")
                    if (gameManager.isValidMove(playedCards)) {
                        TRACE(DEBUG, tag = "MatView:onPlayCombination") { "✅ Move is valid! Playing: $playedCards" }
                        val playMoveResult = gameManager.playCombination(playedCards, cardToKeep)
                        selectedCards.clear()
                    } else {
                        TRACE(FATAL, tag = "MatView:onPlayCombination") { "❌ Invalid move!" }
                    }
                },
                onWithdrawCards = { cardsToWithdraw ->
                    TRACE(DEBUG) { "Withdraw Cards: $cardsToWithdraw" }
                    selectedCards.removeAll(cardsToWithdraw)
                    currentPlayer.hand.cards.addAll(cardsToWithdraw)
                },
                onSkip = { gameManager.processSkip() },
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
                onSortMode = toggleSortMode,
                onSelectCard = { card ->
                    if (selectedCards.contains(card)) {
                        selectedCards.remove(card)
                        val currentHand = currentPlayer.hand.copy().apply { addCard(card) }
                        gameManager.updatePlayerHand(gameState.currentPlayerIndex, currentHand)
                        TRACE(DEBUG, tag = "HandView:onSelectCard") { "Card unselected: ${card.value}, ${card.color}" }
                    } else {
                        val currentHand = currentPlayer.hand.copy()
                        if (currentHand.removeCard(card)) {
                            selectedCards.add(card)
                            gameManager.updatePlayerHand(gameState.currentPlayerIndex, currentHand)
                            TRACE(DEBUG, tag = "HandView:onSelectCard") { "Card selected: ${card.value}, ${card.color}" }
                        } else {
                            TRACE(ERROR, tag = "HandView:onSelectCard") { "Failed to remove card: ${card.value}, ${card.color}" }
                        }
                    }
                }
            )
        }
    }

    // ── Centralized Dialog Observer ──
    if (gameState.gameEvent != null) {
        when (val event = gameState.gameEvent) {
            is AppEvent.GameEvent.CardSelection -> CardSelectionDialog(event = event)
            is AppEvent.GameEvent.RoundOver -> RoundOverDialog(event = event, onExit = { gameManager.startNewRound() })
            is AppEvent.GameEvent.GameOver -> GameOverDialog(event = event, onExit = onEndGame)
            is AppEvent.PlayerEvent.PlayerLeft -> PlayerLeftDialog(event = event)
            is AppEvent.PlayerEvent.ChatMessage -> ChatMessageDialog(event = event)
            is AppEvent.GameEvent.QuitGame -> QuitGameDialog(onConfirm = onQuitGame, onCancel = {})
            else -> TRACE(FATAL) { "Unknown event type: ${gameState.gameEvent}" }
        }
    }

    LaunchedEffect(gameState.turnId) {
        if (currentPlayer.playerType == PlayerType.AI) {
            TRACE(INFO) { "AI will play in ${AI_THINKING_DURATION_MS / 1000} seconds..." }
            kotlinx.coroutines.delay(AI_THINKING_DURATION_MS)
            gameManager.processAIMove()
        }
    }
}

// --- PREVIEW STUB VIEWMODEL ---
class FakeGameViewModelForPreview(
    initialGameState: GameState,
    initialSavedPlayers: List<SavedPlayer> = listOf(SavedPlayer("PreviewUser", "👤", PlayerType.LOCAL)),
    initialPointLimit: Int = 100
) : IGameViewModelPreview {
    private val _gameState = mutableStateOf(initialGameState)
    override val gameState: State<GameState> = _gameState

    private val _savedPlayers = mutableStateOf(initialSavedPlayers)
    override val savedPlayers: State<List<SavedPlayer>> = _savedPlayers

    private val _savedPointLimit = mutableStateOf(initialPointLimit)
    override val savedPointLimit: State<Int> = _savedPointLimit

    override fun updateGameState(newState: GameState) {
        _gameState.value = newState
    }
    override fun updatePlayerHand(playerIndex: Int, newHand: Hand) {
        // Not used in preview, but could be mocked for interactivity if needed.
    }
    override fun savePlayers(players: List<SavedPlayer>) {
        _savedPlayers.value = players
    }
    override fun savePointLimit(limit: Int) {
        _savedPointLimit.value = limit
    }
}

// --- PREVIEW FUNCTION ---
@Preview(
    name = "Landscape MainScreen Preview",
    widthDp = 800,
    heightDp = 400,
    showBackground = true
)
@Composable
fun PreviewMainScreen() {
    val dummyPlayers = listOf(
        LocalPlayer(
            id = "1",
            name = "Alice",
            avatar = "",
            hand = Hand(
                mutableStateListOf(
                    Card(2, "RED"), Card(3, "RED"), Card(4, "GREEN"),
                    Card(3, "MOCHA"), Card(3, "PINK"), Card(3, "GREEN"),
                    Card(2, "BLUE"), Card(5, "ORANGE"), Card(4, "RED")
                )
            )
        ),
        AIPlayer(
            id = "2",
            name = "Bob",
            avatar = "",
            score = 0,
            hand = Hand(
                mutableStateListOf(
                    Card(2, "BLUE"),
                    Card(5, "MOCHA")
                )
            )
        ),
        AIPlayer(
            id = "3",
            name = "Carol",
            avatar = "",
            hand = Hand(
                mutableStateListOf(
                    Card(2, "PINK"),
                    Card(3, "MOCHA"),
                    Card(4, "GREEN")
                )
            )
        )
    )

    val dummyPlaymat = Combination(
        mutableStateListOf(
            Card(3, "RED"),
            Card(3, "MOCHA")
        )
    )

    val dummySelectedCards = mutableStateListOf<Card>().apply {
        addAll(listOf(Card(4, "GREEN"), Card(5, "PINK")))
    }

    val dummyDiscardPile = mutableStateListOf<Card>().apply {
        addAll(listOf(Card(2, "BLUE"), Card(3, "ORANGE")))
    }

    val dummyDeck = mutableStateListOf<Card>().apply {
        addAll(listOf(Card(2, "RED"), Card(3, "RED"), Card(4, "RED")))
    }

    val dummyGameState = GameState(
        playerId = dummyPlayers[0].id,
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
        gameEvent = null,
        turnId = 1
    )

    val dummySavedPlayers = listOf(SavedPlayer("PreviewUser", "👤", PlayerType.LOCAL))

    // 👇 This is the key difference!
    val fakeViewModel = FakeGameViewModelForPreview(
        initialGameState = dummyGameState,
        initialSavedPlayers = dummySavedPlayers,
        initialPointLimit = GAME_DEFAULT_POINT_LIMIT
    )

    NidoTheme {
        CompositionLocalProvider(LocalGameManager provides FakeGameManager()) {
            MainScreen(
                onEndGame = {},
                onQuitGame = {},
                viewModel = fakeViewModel // Pass the fake ViewModel!
            )
        }
    }
}
