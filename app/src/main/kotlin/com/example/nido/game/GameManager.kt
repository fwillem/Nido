package com.example.nido.game

import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.game.rules.GameRules
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.example.nido.data.model.PlayerActionType
import com.example.nido.data.model.Hand
import com.example.nido.events.AppDialogEvent
import com.example.nido.events.GameDialogEvent
import com.example.nido.game.engine.GameEventDispatcher
import com.example.nido.replay.GameRecorder
import kotlin.Int
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import java.util.concurrent.atomic.AtomicBoolean

private val isDispatching = AtomicBoolean(false)


object GameManager : IGameManager {
    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Correction de la signature de l'interface
    override fun initialize(viewModel: GameViewModel) { }


    /**
     * Overall game initialization.
     * This function now only sets up the base state (players, point limit, deck, startingIndex) without dealing cards.
     * Then it calls startNewRound() to handle round-specific initialization.
     */
    override fun startNewGame(selectedPlayers: List<Player>, selectedPointLimit: Int, doNotAutoPlayAI: Boolean, AITimerDuration: Int) {
        TRACE(DEBUG) { "selectedPlayers: $selectedPlayers, selectedPointLimit: $selectedPointLimit, doNotAutoPlayAI: $doNotAutoPlayAI, AITimerDuration: $AITimerDuration" }

        // Start a new recoding session
        GameRecorder.startNewSession()


        // Choose a random starting player.
        // TODO For debug we will simplify, the right value is :  val startingPlayerIndex = (0 until selectedPlayers.size).random()
        // val startingPlayerIndex = (0 until selectedPlayers.size).random()
        // val startingPlayerIndex = -1 // Start by the Human Player
        val startingPlayerIndex = (0 until selectedPlayers.size).random()


        val initializedPlayers =
            GameRules.initializePlayerScores(selectedPlayers, selectedPointLimit)


        // Set up initial state
        val initialState = GameState(
            players = initializedPlayers,
            doNotAutoPlayAI = doNotAutoPlayAI,
            pointLimit = selectedPointLimit,
            startingPlayerIndex = startingPlayerIndex,
            currentPlayerIndex = startingPlayerIndex,
            aiTimerDuration = AITimerDuration
        )
        _gameState.value = initialState
        TRACE(INFO) { "Initial gameState set: ${gameState.value}" }

        // Start the first round (this will shuffle the deck and deal cards).
        startNewRound()
    }

    /**
     * Starts a new round.
     * 🔄 Reshuffles the existing deck (no need to re-create it) and deals cards to each player.
     * 🏁 Updates startingIndex to be the next player after the one who started the previous round.
     */
    override fun startNewRound() {
        TRACE(DEBUG) { "startNewRound()" }
        dispatcher.enqueueEvent(GameEvent.NewRoundStarted)
    }


    private fun getCurrentPlayer(): Player =
        gameState.value.players[gameState.value.currentPlayerIndex]

    override fun skipTurn() {
        TRACE(DEBUG) { "skipTurn()" }
        dispatcher.enqueueEvent(GameEvent.PlayerSkipped)
    }

    /**
     * Play a combination of cards.Returns true if the player won.
     */
    override fun playCombination(
        selectedCards: List<Card>,
        cardToKeep: Card?
    ) {
        val currentGameState = gameState.value

        if (selectedCards.isEmpty()) {
            TRACE(FATAL) { " No cards selected" }
            return
        }

        if (selectedCards.isEmpty()) {
            TRACE(FATAL) { "No cards selected" }
            return
        }

        val event = GameEvent.CardPlayed(
            playerId = getCurrentPlayer().id,
            playedCards = selectedCards,
            cardKept = cardToKeep,
        )

        dispatcher.enqueueEvent(event)
    }


    private fun nextTurn() {
        val currentGameState = gameState.value
        val nextIndex = (currentGameState.currentPlayerIndex + 1) % currentGameState.players.size
        updateGameState(
            currentPlayerIndex = nextIndex,
            turnId = currentGameState.turnId + 1
        )
        val nextPlayer = gameState.value.players[nextIndex]
        TRACE(DEBUG) { "Player is now ${nextPlayer.name}($nextIndex)" }

    }

    override fun getAIMove() {
        val aiPlayer = getCurrentPlayer()
        if (aiPlayer.playerType == PlayerType.AI) {
            TRACE(DEBUG) { "AI is playing (${aiPlayer.name})" }

                val playerAction = aiPlayer.play(this)

                if (playerAction.actionType == PlayerActionType.PLAY) {
                    // Check if combination is null; if so, log a fatal error.
                    if (playerAction.combination == null) {
                        TRACE(FATAL) { "Combination cannot be null when actionType is PLAY for ${aiPlayer.name}" }
                    } else {
                        TRACE(DEBUG) { "${aiPlayer.name} is playing: ${playerAction.combination} and is keeping: ${playerAction.cardToKeep}" }
                        // The non-null assertion (!!) is now safe because TRACE(FATAL) will throw if combination is null.
                        playCombination(playerAction.combination!!.cards, playerAction.cardToKeep)
                    }
                } else {
                    TRACE(INFO) { "${aiPlayer.name} has no move !" }
                    skipTurn()
                }
        } else {
            TRACE(ERROR) { "Not AI's turn!" }
        }
    }

    override fun processSkip() {
        val currentPlayer = getCurrentPlayer()
        if (currentPlayer.playerType == PlayerType.AI) {
            TRACE(WARNING) { " AI not supposed to skip via this function" }
        } else {
            TRACE(INFO) { "Local player ${currentPlayer.name} skips" }
            skipTurn()
        }
    }



    override fun isValidMove(selectedCards: List<Card>): Boolean {
        val currentCombination = gameState.value.currentCombinationOnMat
        val selectedCombination = Combination(selectedCards.toMutableList())
        return GameRules.isValidMove(
            currentCombination,
            selectedCombination,
            getCurrentPlayer().hand.cards
        )
    }

    override fun isGameOver(): Boolean {
        return GameRules.isGameOver(gameState.value.players, gameState.value.pointLimit)
    }

    override fun getGameWinners(): List<Player> {
        return GameRules.getGameWinners(gameState.value.players)
    }

    override fun getPlayerRankings(): List<Pair<Player, Int>> {
        return GameRules.getPlayerRankings(gameState.value.players)
    }

    override fun getPlayerHandScores(): List<Pair<Player, Int>> {
        return GameRules.getPlayerHandScores(gameState.value.players)
    }

    override fun getCurrentPlayerHandSize(): Int {
        return getCurrentPlayer().hand.cards.size
    }

    override fun isCurrentPlayerLocal(): Boolean {
        return getCurrentPlayer().playerType == PlayerType.LOCAL
    }

    // This function checks if the player is able to make a valid move
    // In this current implementation, the function needs to check the union of cards in the selectedcard and the hand
    override fun currentPlayerHasValidCombination(): Boolean {
        val currentPlayer = getCurrentPlayer()
        val handCards = currentPlayer.hand.cards

        // All cards are in hand already — just find valid combos from there
        val possibleMoves: List<Combination> = GameRules.findValidCombinations(handCards)
        val playmatCombination = gameState.value.currentCombinationOnMat

        return possibleMoves.any {
            GameRules.isValidMove(playmatCombination, it, handCards)
        }
    }

    override fun setAppDialogEvent(event: AppDialogEvent) {
        updateGameState(appDialogEvent = event)
    }

    override fun clearAppDialogEvent() {
        updateGameState(appDialogEvent = null)
    }

    override fun setGameDialogEvent(event: GameDialogEvent) {
        updateGameState(gameDialogEvent = event)
    }

    override fun clearGameDialogEvent() {
        updateGameState(gameDialogEvent = null)
    }


    private fun launchAITimer(turnId: Int, duration: Int) {
        // TODO TOCHANGE Should not use global Scope
        GlobalScope.launch {
            delay(duration.toLong())
            dispatcher.enqueueEvent(GameEvent.AITimerExpired(turnId))
        }
    }

    // Inside GameManager (keep them private)
    private fun enqueueSound(effect: SoundEffect) {
        val cur = gameState.value
        updateGameState(pendingSounds = cur.pendingSounds + effect) // only the changed field
    }

    override fun consumeSound(effect: SoundEffect) {
        val cur = gameState.value
        updateGameState(pendingSounds = cur.pendingSounds - effect) // only the changed field
    }

    // 🟢 Add helpers (keep enqueue private; expose consume for UI)
    private fun enqueueMusic(cmd: MusicCommand) {
        val cur = gameState.value
        updateGameState(pendingMusic = cur.pendingMusic + cmd)
    }

    /** Public so UI handler can remove the command after executing it. */
    override fun consumeMusic(cmd: MusicCommand) {
        val cur = gameState.value
        updateGameState(pendingMusic = cur.pendingMusic - cmd)
    }



    override fun updatePlayerHand(playerIndex: Int, hand: Hand) {
        val currentGameState = gameState.value
        if (playerIndex in currentGameState.players.indices) {
            val updatedPlayers = currentGameState.players.mapIndexed { index, player ->
                if (index == playerIndex) player.copy(hand = hand) else player
            }
            updateGameState(players = updatedPlayers)
            TRACE(DEBUG) { "✅ Updated Player($playerIndex) hand: ${hand.cards}" }
        } else {
            TRACE(ERROR) { "⚠️ Invalid playerIndex: $playerIndex" }
        }
    }

    override fun hasPlayedAllRemainingCards(): Boolean {
        val currentPlayer = getCurrentPlayer()
        return currentPlayer.hand.cards.all { it.isSelected }
    }

    /**
     * Updates the entire game state with a new GameState object.
     *
     * ⚠️ This should be the ONLY method used by reducers and event-loop code.
     * Use this for atomic, consistent updates after reducer/event logic.
     */
    fun updateGameState(newState: GameState) {
        _gameState.value = newState
    }

    /**
     * Updates the game state with individual properties (partial update).
     *
     * Intended for use by UI components, dialogs, or code that needs to modify only
     * a specific part of the state (e.g. selected cards, dialog events).
     *
     * 🚫 Do NOT use this from reducer/event-loop logic, as it may cause bugs if
     * fields are omitted.
     */
    fun updateGameState(
        players: List<Player> = gameState.value.players,
        deck: androidx.compose.runtime.snapshots.SnapshotStateList<Card> = gameState.value.deck,
        discardPile: androidx.compose.runtime.snapshots.SnapshotStateList<Card> = gameState.value.discardPile,
        startingPlayerIndex: Int = gameState.value.startingPlayerIndex,
        currentPlayerIndex: Int = gameState.value.currentPlayerIndex,
        currentPlayerId: String = gameState.value.currentPlayerId,
        currentCombinationOnMat: Combination = gameState.value.currentCombinationOnMat,
        skipCount: Int = gameState.value.skipCount,
        turnInfo: TurnInfo = gameState.value.turnInfo,
        pointLimit: Int = gameState.value.pointLimit,
        aiTimerDuration: Int = gameState.value.aiTimerDuration,
        soundEffectVolume : SoundVolume = gameState.value.soundEffectVolume,
        soundMusicVolume : SoundVolume = gameState.value.soundMusicVolume,
        pendingSounds: List<SoundEffect> = gameState.value.pendingSounds,
        pendingMusic: List<MusicCommand> = gameState.value.pendingMusic,
        appDialogEvent: AppDialogEvent? = gameState.value.appDialogEvent,
        gameDialogEvent: GameDialogEvent? = gameState.value.gameDialogEvent,
        turnId: Int = gameState.value.turnId,


        doNotAutoPlayAI: Boolean = gameState.value.doNotAutoPlayAI
    ) {
        _gameState.value = gameState.value.copy(
            players = players,
            deck = deck,
            discardPile = discardPile,
            startingPlayerIndex = startingPlayerIndex,
            currentPlayerIndex = currentPlayerIndex,
            currentPlayerId = currentPlayerId,
            currentCombinationOnMat = currentCombinationOnMat,
            skipCount = skipCount,
            turnInfo = turnInfo,
            pointLimit = pointLimit,
            aiTimerDuration = aiTimerDuration,
            appDialogEvent = appDialogEvent,
            soundEffectVolume = soundEffectVolume,
            soundMusicVolume = soundMusicVolume,
            pendingSounds = pendingSounds,
            pendingMusic = pendingMusic,
            gameDialogEvent = gameDialogEvent,
            turnId = turnId,
            turnHintMsg = gameState.value.turnHintMsg,
            bannerMsg = gameState.value.bannerMsg,
            doNotAutoPlayAI = doNotAutoPlayAI
        )
    }

    private val dispatcher = GameEventDispatcher(
        getState = { gameState.value },
        updateState = { updateGameState(it) },
        handleSideEffect = ::handleSideEffect,
        reducer = ::gameReducer
    )

    private fun handleSideEffect(effect: GameSideEffect) {
        when (effect) {
            is GameSideEffect.StartAITimer -> launchAITimer(effect.turnId, this.gameState.value.aiTimerDuration)
            is GameSideEffect.ShowDialog -> setGameDialogEvent(effect.dialog)
            is GameSideEffect.GetAIMove -> getAIMove()
            is GameSideEffect.PlaySound -> enqueueSound(effect.effect)
            is GameSideEffect.PlayMusic -> enqueueMusic(MusicCommand.Play(effect.track, effect.loop))
            is GameSideEffect.StopMusic -> enqueueMusic(MusicCommand.Stop)

        }
    }

}

// Internal helper function to expose GameManager as IGameManager within the module.
internal fun getGameManagerInstance(): IGameManager = GameManager
