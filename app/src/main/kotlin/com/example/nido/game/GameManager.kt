package com.example.nido.game

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.data.repository.DeckRepository
import com.example.nido.game.rules.GameRules
import com.example.nido.utils.Constants
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.example.nido.data.model.PlayerActionType
import com.example.nido.data.model.Hand
import com.example.nido.events.AppEvent
import kotlin.Int
import com.example.nido.game.events.GameEvent
import com.example.nido.utils.Debug
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    override fun startNewGame(selectedPlayers: List<Player>, selectedPointLimit: Int, doNotAutoPlayAI: Boolean) {
        TRACE(DEBUG) { "selectedPlayers: $selectedPlayers, selectedPointLimit: $selectedPointLimit" }



        // Choose a random starting player.
        // TODO For debug we will simplify, the right value is :  val startingPlayerIndex = (0 until selectedPlayers.size).random()
        val startingPlayerIndex = -1

        val initializedPlayers =
            GameRules.initializePlayerScores(selectedPlayers, selectedPointLimit)


        // Set up initial state
        val initialState = GameState(
            players = initializedPlayers,
            doNotAutoPlayAI = doNotAutoPlayAI,
            pointLimit = selectedPointLimit,
            startingPlayerIndex = startingPlayerIndex,
            currentPlayerIndex = startingPlayerIndex,
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
        dispatchEvent(GameEvent.NewRoundStarted)
    }


    private fun getCurrentPlayer(): Player =
        gameState.value.players[gameState.value.currentPlayerIndex]

    override fun skipTurn() {
        TRACE(DEBUG) { "skipTurn()" }
        dispatchEvent(GameEvent.PlayerSkipped)
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

        dispatchEvent(event)
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

    override fun processAIMove() {
        val currentPlayer = getCurrentPlayer()
        if (currentPlayer.playerType == PlayerType.AI) {
            TRACE(DEBUG) { "AI is playing (${currentPlayer.name})" }
            handleAIMove(currentPlayer)
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

    private fun handleAIMove(aiPlayer: Player) {
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

    private fun launchAITimer(turnId: Int) {
        GlobalScope.launch {
            delay(Constants.AI_THINKING_DURATION_MS)
            dispatchEvent(GameEvent.AITimerExpired(turnId))
        }
    }
/*
    override fun withdrawCardsFromMat(cardsToWithdraw: List<Card>) {
        val currentGameState = gameState.value
        val currentPlayer = getCurrentPlayer()

        TRACE(DEBUG) { "Withdrawing cards $cardsToWithdraw from ${currentPlayer.name}'s hand." }

        // Create a copy of the current player's hand and add back the withdrawn cards.
        val updatedHand = currentPlayer.hand.copy()
        cardsToWithdraw.forEach { card ->
            updatedHand.addCard(card)
        }
        val updatedPlayer = currentPlayer.copy(hand = updatedHand)

        // Update the players list with the updated player.
        val updatedPlayers = currentGameState.players.toMutableList().apply {
            this[currentGameState.currentPlayerIndex] = updatedPlayer
        }

        // Withdraw the cards from selectedCards.
        // We create a new mutable list and remove the withdrawn cards.
        val updatedSelectedCards = mutableStateListOf<Card>().apply {
            addAll(currentGameState.selectedCards)
        }
        updatedSelectedCards.removeAll(cardsToWithdraw)

        // Create the new game state with the updated players and selectedCards.
        val updatedState = currentGameState.copy(
            players = updatedPlayers,
            selectedCards = updatedSelectedCards
        )

        getViewModel().updateGameState(updatedState)
        TRACE(DEBUG) { "Withdrawn cards ${cardsToWithdraw.joinToString()} returned to ${currentPlayer.name}'s hand." }
    }

 */

    override fun setDialogEvent(event: AppEvent) {
        updateGameState(gameEvent = event)
    }
    override fun clearDialogEvent() {
        updateGameState(gameEvent = null)
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

    // Permet de mettre à jour l'état du jeu depuis le ViewModel ou d'autres parties de l'app
    fun updateGameState(
        players: List<Player> = gameState.value.players,
        deck: androidx.compose.runtime.snapshots.SnapshotStateList<Card> = gameState.value.deck,
        discardPile: androidx.compose.runtime.snapshots.SnapshotStateList<Card> = gameState.value.discardPile,
        startingPlayerIndex: Int = gameState.value.startingPlayerIndex,
        currentPlayerIndex: Int = gameState.value.currentPlayerIndex,
        currentCombinationOnMat: Combination = gameState.value.currentCombinationOnMat,
        skipCount: Int = gameState.value.skipCount,
        turnInfo: TurnInfo = gameState.value.turnInfo,
        playerId: String = gameState.value.playerId,
        pointLimit: Int = gameState.value.pointLimit,
        soundOn: Boolean = gameState.value.soundOn,
        gameEvent: AppEvent? = gameState.value.gameEvent,
        turnId: Int = gameState.value.turnId,
        doNotAutoPlayAI: Boolean = gameState.value.doNotAutoPlayAI
    ) {
        _gameState.value = gameState.value.copy(
            players = players,
            deck = deck,
            discardPile = discardPile,
            startingPlayerIndex = startingPlayerIndex,
            currentPlayerIndex = currentPlayerIndex,
            currentCombinationOnMat = currentCombinationOnMat,
            skipCount = skipCount,
            turnInfo = turnInfo,
            playerId = playerId,
            pointLimit = pointLimit,
            soundOn = soundOn,
            gameEvent = gameEvent,
            turnId = turnId,
            doNotAutoPlayAI = doNotAutoPlayAI
        )
    }

    private fun dispatchEvent(event: GameEvent) {
        /*
            We protect dispatchEvent against reeentrance, this will not make our application 100% save because UI components aren't all using dispatchevent
            but at least external event (network, timer, other system stuff will
         */
        if (!isDispatching.compareAndSet(false, true)) {
            TRACE(FATAL) { "RE-ENTRANCE detected in dispatchEvent!" }
        }
        try {

            val currentState = gameState.value
            val result = gameReducer(currentState, event)
            updateGameState(
                players = result.newState.players,
                deck = result.newState.deck,
                discardPile = result.newState.discardPile,
                startingPlayerIndex = result.newState.startingPlayerIndex,
                currentPlayerIndex = result.newState.currentPlayerIndex,
                currentCombinationOnMat = result.newState.currentCombinationOnMat,
                skipCount = result.newState.skipCount,
                turnInfo = result.newState.turnInfo,
                playerId = result.newState.playerId,
                pointLimit = result.newState.pointLimit,
                soundOn = result.newState.soundOn,
                gameEvent = result.newState.gameEvent,
                turnId = result.newState.turnId,
                doNotAutoPlayAI = result.newState.doNotAutoPlayAI
            )

            result.sideEffects.forEach { effect ->
                when (effect) {
                    is GameSideEffect.StartAITimer -> launchAITimer(effect.turnId)
                    is GameSideEffect.ShowDialog -> setDialogEvent(effect.dialog)
                }
            }

            result.followUpEvents.forEach { followUp -> dispatchEvent(followUp) }

        }  finally {
            isDispatching.set(false)
        }
    }
}

// Internal helper function to expose GameManager as IGameManager within the module.
internal fun getGameManagerInstance(): IGameManager = GameManager
