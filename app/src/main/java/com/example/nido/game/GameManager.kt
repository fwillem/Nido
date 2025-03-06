package com.example.nido.game

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.State
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




object GameManager {
    private var gameViewModel: GameViewModel? = null // ✅ Nullable until initialized
    val gameState: State<GameState>
        get() = getViewModel().gameState  // ✅ Use ViewModel’s state


    fun initialize(viewModel: GameViewModel) {
        if (gameViewModel != null) {
            TRACE(FATAL) { "GameManager is already initialized!" } // 🚨 Prevent multiple initializations
        }
        gameViewModel = viewModel
    }

    private fun getViewModel(): GameViewModel {
        return gameViewModel ?: throw IllegalStateException("GameManager has not been initialized!") // 🚨 Prevent usage before initialization
    }

    fun startNewRound() {
        val currentGameState = gameState.value

        val newDeck = currentGameState.deck.shuffle()

        val mutableDeck = mutableStateListOf<Card>().apply { addAll(newDeck) }
        // Re-deal cards to players (if that’s part of your round logic)
        var newState = currentGameState.copy(
            deck = mutableDeck,
            currentCombinationOnMat = Combination(mutableListOf()),
            skipCount = 0,
            discardPile = mutableStateListOf(),
        )
        newState = dealCards(newState)
        getViewModel().updateGameState(newState)
        TRACE(INFO) { "New round started: ${getViewModel().gameState}" }
    }


    fun startNewGame(selectedPlayers: List<Player>, selectedPointLimit: Int) {

        TRACE (DEBUG) { "selectedPlayers : $selectedPlayers, selectedPointLimit : $selectedPointLimit" }

        val removedColors = if (selectedPlayers.size <= Constants.GAME_REDUCED_COLOR_THRESHOLD) {
            Constants.DECK_REMOVED_COLORS
        } else {
            emptySet()
        }

        val deck = DeckRepository.generateDeck(shuffle = true, removedColors = removedColors)

        val mutableDeck = mutableStateListOf<Card>().apply { addAll(deck) }

        var newGameState = GameState(
            players = selectedPlayers,
            pointLimit = selectedPointLimit,
            deck = mutableDeck,
            currentPlayerIndex = 0,
            currentCombinationOnMat = Combination(mutableListOf()),
            discardPile = mutableStateListOf(),
            skipCount = 0,  // Initialize skipCount to 0
            screen = GameScreens.PLAYING
        )


        var newGameState = GameState(
            players = selectedPlayers,
            pointLimit = selectedPointLimit,
            deck = mutableDeck,
            startingPlayerIndex = 0,
            currentPlayerIndex = 0,
            currentCombinationOnMat = Combination(mutableListOf()),
            discardPile = mutableStateListOf(),
            skipCount = 0,  // Initialize skipCount to 0
            screen = GameScreens.PLAYING
        )


        // Deal the cards across all players and update the game state.
        newGameState = dealCards(newGameState)
        gameViewModel?.updateGameState(newGameState)
            ?: TRACE (ERROR) { "ERROR: GameViewModel is not initialized!" }

        TRACE (INFO) { "Initial gameState ${getViewModel().gameState}" }
    }


    private fun dealCards(state: GameState): GameState {
        val mutableDeck = state.deck.toMutableList()
        val mutablePlayers = state.players.map { player ->
            val updatedHand = player.hand.copy()
            repeat(Constants.HAND_SIZE) {
                if (mutableDeck.isNotEmpty()) {
                    val card = mutableDeck.removeAt(0)
                    updatedHand.addCard(card)
                } else {
                    TRACE (FATAL) { "Deck is empty before dealing all cards!" }
                }
            }
            player.copy(hand = updatedHand)
        }

        // Trace each player's name and their hand
        mutablePlayers.forEach { player ->

            TRACE(INFO) { "$player.name's hand:" + player.hand.cards.joinToString(", ") { card -> "${card.value} ${card.color}" } }
        }

        return state.copy(
            players = mutablePlayers,
            deck = mutableStateListOf<Card>().apply { addAll(mutableDeck) }
        )
    }

    private fun getCurrentPlayer(): Player = gameState.value.players[gameState.value.currentPlayerIndex]

    fun skipTurn() {
        TRACE(DEBUG) { "${getCurrentPlayer().name} is skipping turn" }
        val currentGameState = gameState.value
        val newSkipCount = currentGameState.skipCount + 1

        if (newSkipCount >= currentGameState.players.size) {
            // All players have skipped: discard the current playmat and allow the same player to replay.
            TRACE(INFO) { "All players skipped! Discarding current playmat and allowing ${getCurrentPlayer().name} to replay." }
            val discardedCards = currentGameState.currentCombinationOnMat.cards
            val newDiscardPile = mutableStateListOf<Card>().apply {
                addAll(currentGameState.discardPile)
                addAll(discardedCards)
            }
            // Reset currentCombinationOnMat and skipCount, but keep currentPlayerIndex unchanged.
            val updatedState = currentGameState.copy(
                currentCombinationOnMat = Combination(mutableListOf()),
                discardPile = newDiscardPile,
                skipCount = 0
            )
            getViewModel().updateGameState(updatedState)
        } else {
            // Just update the skip count and move on to the next player's turn.
            TRACE(VERBOSE) { "updating skip count to $newSkipCount" }

            val updatedState = currentGameState.copy(skipCount = newSkipCount)
            getViewModel().updateGameState(updatedState)
            nextTurn()
        }
    }

    /**
     * Play a combination of cards.Returns true if the player won.
     */
    fun playCombination(selectedCards: List<Card>, cardToKeep: Card?) : Boolean {
        val currentGameState = gameState.value

        if (selectedCards.isEmpty()) {
            TRACE(FATAL) { " No cards selected" }
            return false
        }

        // Create the new combination based on selected cards.
        val currentCombination = currentGameState.currentCombinationOnMat
        val newCombination = Combination(selectedCards.toMutableList())

        // Validate the move.
        if (!GameRules.isValidMove(currentCombination, newCombination)) {
            TRACE(FATAL) { "Invalid combination! Move rejected." } // THis shall not happen here since it has been checked before in MatView
            return false
        }


        /**
         * Update the current player's hand by removing the played cards (note that for human players, card has already been removed by HandView)
         */

        val updatedHand = getCurrentPlayer().hand.copy().apply { removeCombination(newCombination) }
        val updatedPlayers = currentGameState.players.toMutableList().apply {
            this[currentGameState.currentPlayerIndex] = getCurrentPlayer().copy(hand = updatedHand)
        }

        // We need to figure out here is the player won
        if (GameRules.hasPlayerWonTheRound(getCurrentPlayer())) {
            TRACE(INFO) { "${getCurrentPlayer().name} is playing: $newCombination " }
            TRACE(INFO) { "😍 ${getCurrentPlayer().name}  😎 won! " }

            /**
             * The player won the round !
             * Update the scores
             * Understand if the game is over
             */
            GameRules.updatePlayersScores(updatedPlayers)
            val gameOver = GameRules.isGameOver(updatedPlayers, currentGameState.pointLimit)

            if (gameOver) {
                TRACE(INFO) { "Game is over! 🍾" }
            } else {
                TRACE(INFO) { "We need to Start a new round" }
                startNewRound()
            }

            return true
        } else {
            // Build a new discard pile:
            // It consists of the existing discard pile plus the cards from the current combination
            // excluding the card chosen by the player to keep.
            val discardedCards = currentCombination.cards.filter { it != cardToKeep }

            val newDiscardPile = mutableStateListOf<Card>().apply {
                addAll(currentGameState.discardPile)
                addAll(discardedCards)
            }

            TRACE(INFO) { "${getCurrentPlayer().name} is playing: $newCombination and is keeping: $cardToKeep, $discardedCards moves to discard pile" }


            // If a card was chosen to keep, add it back to the player's hand.
            cardToKeep?.let { updatedHand.addCard(it) }

            // Update the game state.
            val updatedState = currentGameState.copy(
                players = updatedPlayers,
                currentCombinationOnMat = newCombination,
                discardPile = newDiscardPile,
                skipCount = 0
            )

            getViewModel().updateGameState(updatedState)

            nextTurn()

            return false
        }
    }


    private fun nextTurn() {
        val currentGameState = gameState.value
        val nextIndex = (currentGameState.currentPlayerIndex + 1) % currentGameState.players.size

        val updatedState = currentGameState.copy(currentPlayerIndex = nextIndex)


        getViewModel().updateGameState(updatedState) // ✅ Safe access to ViewModel

        val nextPlayer = updatedState.players[nextIndex]
        TRACE(DEBUG) { "Player is now ${nextPlayer.name}($nextIndex)" }

    }

    fun processAIMove() {
        val currentPlayer = getCurrentPlayer()
        if (currentPlayer.playerType == PlayerType.AI) {
            TRACE(DEBUG) {"AI is playing (${currentPlayer.name})" }
            handleAIMove(currentPlayer)
        } else {
            TRACE(ERROR) {"Not AI's turn!" }
        }
    }
    fun processSkip() {
        val currentPlayer = getCurrentPlayer()
        if (currentPlayer.playerType == PlayerType.AI) {
            TRACE(FATAL) {" AI not supposed to skip via this function" }
        } else {
            TRACE(INFO) {"Local player ${currentPlayer.name} skips" }
            skipTurn()
        }
    }

    private fun handleAIMove(aiPlayer: Player) {
        val playerAction = aiPlayer.play(this)

        if (playerAction.actionType == PlayerActionType.PLAY) {
            // Check if combination is null; if so, log a fatal error.
            if (playerAction.combination == null) {
                TRACE(FATAL) { "Combination cannot be null when actionType is PLAY for ${aiPlayer.name}" }
            }
            TRACE(DEBUG) { "${aiPlayer.name} is playing: ${playerAction.combination} and is keeping: ${playerAction.cardToKeep}" }
            // The non-null assertion (!!) is now safe because TRACE(FATAL) will throw if combination is null.
            playCombination(playerAction.combination!!.cards, playerAction.cardToKeep)
        } else {
            TRACE(INFO) { "${aiPlayer.name} has no move !" }
            skipTurn()
        }
    }

    fun isValidMove(selectedCards: List<Card>): Boolean {
        val currentCombination = gameState.value.currentCombinationOnMat
        val selectedCombination = Combination(selectedCards.toMutableList())
        return GameRules.isValidMove(currentCombination, selectedCombination)
    }

    fun isGameOver(): Boolean {
        return GameRules.isGameOver(gameState.value.players, gameState.value.pointLimit)
    }

    fun getGameWinners(): List<Player> {
        return GameRules.getGameWinners(gameState.value.players)
    }

    fun getPlayerRankings(): List<Pair<Player, Int>> {
        return GameRules.getPlayerRankings(gameState.value.players)
    }
}
