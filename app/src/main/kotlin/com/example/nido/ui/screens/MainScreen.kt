package com.example.nido.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nido.R
import com.example.nido.data.SavedPlayer
import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Hand
import com.example.nido.data.model.PlayerType
import com.example.nido.events.GameDialogEvent
import com.example.nido.game.FakeGameManager
import com.example.nido.game.GameState
import com.example.nido.game.LocalPlayer
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
import com.example.nido.utils.Constants.AI_THINKING_DURATION_DEFAULT
import com.example.nido.utils.Constants.GAME_DEFAULT_POINT_LIMIT
import com.example.nido.utils.Debug
import com.example.nido.utils.SortMode
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*

@Composable
fun MainScreen(
    onEndGame: () -> Unit,
    onQuitGame: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IGameViewModelPreview,
    debug: Debug // This is a mandatory parameter
) {
    val gameManager = LocalGameManager.current  // ✅ Retrieve injected GameManager
    val gameState by viewModel.gameState.collectAsState()

    println("MainScreen: gameState IN = $gameState.players")
    // Use derivedStateOf for values that depend on gameState
    val currentPlayer by remember {
        derivedStateOf {
            require(gameState.players.isNotEmpty()) { "GameState.players is empty – this should not happen!" }
            gameState.players[gameState.currentPlayerIndex]
        }
    }
    println("MainScreen: gameState OUT = $gameState.players")

    // This current Hand of all players, even AI or remote players
    val currentHand by remember { derivedStateOf { currentPlayer.hand.cards } }

    // This is the LocalPlayer view,
    val localPlayerHand by remember {
        derivedStateOf {
            gameState.players.firstOrNull { it.playerType == PlayerType.LOCAL }?.hand?.cards.orEmpty()
        }
    }

    val discardPile by remember { derivedStateOf { gameState.discardPile } }
    val players by remember { derivedStateOf { gameState.players } }
    val currentTurnIndex by remember { derivedStateOf { gameState.currentPlayerIndex } }
    val playmat by remember { derivedStateOf { gameState.currentCombinationOnMat } }

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
            stringResource(R.string.sort_mode, sortMode.name) to { toggleSortMode() },
            stringResource(R.string.quit) to {
                gameManager.setGameDialogEvent(GameDialogEvent.QuitGame)
            },
        )

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column {
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
                    .weight(0.7f)
                    .background(NidoColors.MatViewBackground),
                contentAlignment = Alignment.Center
            ) {

                val playmatSnapshotList = playmat?.cards?.let { cardList ->
                    mutableStateListOf<Card>().apply { addAll(cardList) }
                } ?: mutableStateListOf()






                MatView(
                    playmat = playmatSnapshotList,
                    debug = debug,
                    currentPlayerHand = currentHand,
                    onPlayCombination = { playedCards, cardToKeep ->
                        if (gameManager.isValidMove(playedCards)) {
                            TRACE(
                                DEBUG,
                                tag = "MatView:onPlayCombination"
                            ) { " Move is valid! Playing: $playedCards" }


                            val playMoveResult =
                                gameManager.playCombination(playedCards, cardToKeep)

                            //  Deselect played cards before they go to the mat
                            playedCards.forEach { it.isSelected = false }

                        } else {
                            TRACE(FATAL, tag = "MatView:onPlayCombination") { "❌ Invalid move!" }
                        }
                    },
                    onWithdrawCards = { cardsToWithdraw ->
                        TRACE(DEBUG) { "Withdraw Cards: $cardsToWithdraw" }

                        // Unselect the cards
                        cardsToWithdraw.forEach { it.isSelected = false }

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
                    hand = Hand(mutableStateListOf<Card>().apply { addAll(localPlayerHand) }),
                    cardWidth = Constants.CARD_ON_HAND_WIDTH.dp,
                    cardHeight = Constants.CARD_ON_HAND_HEIGHT.dp,
                    sortMode = sortMode,
                    debug = debug,
                    onDoubleClick = toggleSortMode,
                    onSortMode = toggleSortMode,
                    onSelectCard = { card ->
                        card.isSelected = !card.isSelected

                        TRACE(DEBUG, tag = "HandView:onSelectCard") {
                            val status = if (card.isSelected) "selected" else "unselected"
                            "Card $status: ${card.value}, ${card.color}"
                        }
                    }
                )
            }
        }
    }

    // ── Centralized Dialog Observer ──
    val event = gameState.gameDialogEvent
    if (event != null) {
        when (event) {
            is GameDialogEvent.CardSelection -> CardSelectionDialog(event)
            is GameDialogEvent.RoundOver     -> RoundOverDialog(event) {
                gameManager.clearGameDialogEvent()
                gameManager.startNewRound()
            }
            is GameDialogEvent.GameOver      -> GameOverDialog(event) {
                gameManager.clearGameDialogEvent()
                onEndGame()
            }
            is GameDialogEvent.QuitGame      -> QuitGameDialog(
                onConfirm = { gameManager.clearGameDialogEvent(); onQuitGame() },
                onCancel  = { gameManager.clearGameDialogEvent() }
            )
        }
    }

    /***
     * To Be REMOVED
    LaunchedEffect(gameState.turnId) {
        if (currentPlayer.playerType == PlayerType.AI) {
            TRACE(VERBOSE) { "AI will play in ${AI_THINKING_DURATION_MS / 1000} seconds..." }
            kotlinx.coroutines.delay(AI_THINKING_DURATION_MS)
            gameManager.processAIMove()
        }
    }
     */
}

// --- PREVIEW STUB VIEWMODEL ---
class FakeGameViewModelForPreview(
    initialGameState: GameState,
    initialSavedPlayers: List<SavedPlayer> = listOf(
        SavedPlayer(
            "PreviewUser",
            "👤",
            PlayerType.LOCAL
        )
    ),
    initialPointLimit: Int = 100,
    initialDebug: Debug = Debug()
) : IGameViewModelPreview {
    private val _gameState = kotlinx.coroutines.flow.MutableStateFlow(initialGameState)
    override val gameState: kotlinx.coroutines.flow.StateFlow<GameState> = _gameState

    private val _savedPlayers = kotlinx.coroutines.flow.MutableStateFlow(initialSavedPlayers)
    override val savedPlayers: kotlinx.coroutines.flow.StateFlow<List<SavedPlayer>> = _savedPlayers

    private val _savedPointLimit = kotlinx.coroutines.flow.MutableStateFlow(initialPointLimit)
    override val savedPointLimit: kotlinx.coroutines.flow.StateFlow<Int> = _savedPointLimit

    private val _savedDebug = kotlinx.coroutines.flow.MutableStateFlow(initialDebug)
    override val savedDebug: kotlinx.coroutines.flow.StateFlow<Debug> = _savedDebug

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

    override fun saveDebug(debug: Debug) {
        _savedDebug.value = debug
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
                remember { mutableStateListOf(
                    Card(2, "RED"), Card(3, "RED"), Card(4, "GREEN"),
                    Card(3, "MOCHA"), Card(3, "PINK"), Card(3, "GREEN"),
                    Card(2, "BLUE"), Card(5, "ORANGE"), Card(4, "RED")
                ) }
            )
        ),
        AIPlayer(
            id = "2",
            name = "Bob",
            avatar = "",
            score = 0,
            hand = Hand(
                remember { mutableStateListOf(
                    Card(2, "BLUE"),
                    Card(5, "MOCHA")
                ) }
            )
        ),
        AIPlayer(
            id = "3",
            name = "Carol",
            avatar = "",
            hand = Hand(
                remember { mutableStateListOf(
                    Card(2, "PINK"),
                    Card(3, "MOCHA"),
                    Card(4, "GREEN")
                ) }
            )
        )
    )

    val dummyPlaymat = Combination(
        remember { mutableStateListOf(
            Card(3, "RED"),
            Card(3, "MOCHA")
        ) }
    )

    // FIX: Wrap mutableStateListOf() creation with remember
    val dummySelectedCards = remember { mutableStateListOf<Card>().apply {
        addAll(listOf(Card(4, "GREEN"), Card(5, "PINK")))
    }}

    // FIX: Wrap mutableStateListOf() creation with remember
    val dummyDiscardPile = remember { mutableStateListOf<Card>().apply {
        addAll(listOf(Card(2, "BLUE"), Card(3, "ORANGE")))
    }}

    // FIX: Wrap mutableStateListOf() creation with remember
    val dummyDeck = remember { mutableStateListOf<Card>().apply {
        addAll(listOf(Card(2, "RED"), Card(3, "RED"), Card(4, "RED")))
    }}

    val dummyGameState = GameState(
        pointLimit = 100,
        players = dummyPlayers,
        startingPlayerIndex = 0,
        currentPlayerIndex = 0,
        currentPlayerId = dummyPlayers.first().id,
        currentCombinationOnMat = dummyPlaymat,
        discardPile = dummyDiscardPile,
        deck = dummyDeck,
        skipCount = 0,
        soundOn = true,
        gameDialogEvent = null,
        appDialogEvent = null,
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
                viewModel = fakeViewModel, // Pass the fake ViewModel!,
                debug = Debug(true,true)
            )
        }
    }
}
