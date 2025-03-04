package com.example.nido.game

import androidx.compose.runtime.MutableState
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
import com.example.nido.utils.println



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

    fun startNewGame(selectedPlayers: List<Player>, selectedPointLimit: Int) {

        TRACE (INFO) { "selectedPlayers : $selectedPlayers, selectedPointLimit : $selectedPointLimit" }

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

            val hand = "$player.name's hand:" + player.hand.cards.joinToString(", ") { card -> "${card.value} ${card.color}" }
            TRACE(INFO) { "$hand"}
        }

        return state.copy(
            players = mutablePlayers,
            deck = mutableStateListOf<Card>().apply { addAll(mutableDeck) }
        )
    }

    private fun getCurrentPlayer(): Player = gameState.value.players[gameState.value.currentPlayerIndex]

    fun playCombination(selectedCards: List<Card>, cardToKeep: Card?) {
        val currentGameState = gameState.value

        if (selectedCards.isEmpty()) {
            TRACE(ERROR) { " No cards selected" }
            return
        }

        // Create the new combination based on selected cards.
        val currentCombination = currentGameState.currentCombinationOnMat
        val newCombination = Combination(selectedCards.toMutableList())

        // Validate the move.
        if (!GameRules.isValidMove(currentCombination, newCombination)) {
            TRACE(ERROR) { "Invalid combination! Move rejected." }
            return
        }

        // Update the current player's hand by removing the played cards.
        // For some 'Compose' recomposition reason we have to copy the whole players set
        val updatedHand = getCurrentPlayer().hand.copy().apply { removeCombination(newCombination) }
        val updatedPlayers = currentGameState.players.toMutableList().apply {
            this[currentGameState.currentPlayerIndex] = getCurrentPlayer().copy(hand = updatedHand)
        }

        // Build a new discard pile:
        // It consists of the existing discard pile plus the cards from the current combination
        // excluding the card chosen by the player to keep.
        val newDiscardPile = mutableStateListOf<Card>().apply {
            addAll(currentGameState.discardPile)
            addAll(currentCombination.cards.filter { it != cardToKeep })
        }

        // If a card was chosen to keep, add it back to the player's hand.
        cardToKeep?.let { updatedHand.addCard(it) }

        // Update the game state.
        val updatedState = currentGameState.copy(
            players = updatedPlayers,
            currentCombinationOnMat = newCombination,
            discardPile = newDiscardPile
        )

        getViewModel().updateGameState(updatedState)
        nextTurn()
    }


    private fun nextTurn() {
        val currentGameState = gameState.value
        val nextIndex = (currentGameState.currentPlayerIndex + 1) % currentGameState.players.size

        val updatedState = currentGameState.copy(currentPlayerIndex = nextIndex)
        getViewModel().updateGameState(updatedState) // ✅ Safe access to ViewModel

        val nextPlayer = updatedState.players[nextIndex]
        if (nextPlayer.playerType == PlayerType.AI) {
            handleAIMove(nextPlayer)
        }
    }

    fun processAIMove() {
        val currentPlayer = getCurrentPlayer()
        if (currentPlayer.playerType == PlayerType.AI) {
            handleAIMove(currentPlayer)
        } else {
            TRACE(ERROR) {"Not AI's turn!" }
        }
    }

    private fun handleAIMove(aiPlayer: Player) {
        val bestMove = aiPlayer.play(this)
        bestMove?.let { processMove(it.cards) }
    }

    fun isValidMove(selectedCards: List<Card>): Boolean {
        val currentCombination = gameState.value.currentCombinationOnMat
        val selectedCombination = Combination(selectedCards.toMutableList())
        return GameRules.isValidMove(currentCombination, selectedCombination)
    }

    private fun processMove(selectedCards: List<Card>) {
        if (!isValidMove(selectedCards)) return

        val currentGameState = gameState.value
        val newPlaymat = mutableStateListOf<Card>().apply { addAll(selectedCards) }

        val updatedHand = getCurrentPlayer().hand.copy().apply {
            selectedCards.forEach { removeCard(it) }
        }

        val updatedPlayers = currentGameState.players.toMutableList().apply {
            this[currentGameState.currentPlayerIndex] = getCurrentPlayer().copy(hand = updatedHand)
        }

        val updatedState = currentGameState.copy(
            players = updatedPlayers,
            currentCombinationOnMat = Combination(newPlaymat)
        )

        getViewModel().updateGameState(updatedState) // ✅ Safe access to ViewModel
    }

    private fun checkRoundEnd(): Boolean {
        return gameState.value.players.any { it.hand.isEmpty() }
    }

    fun isGameOver(): Boolean = gameState.value.players.any { it.score >= gameState.value.pointLimit }

    fun getGameWinners(): List<Player> {
        val lowestScore = gameState.value.players.minOfOrNull { it.score } ?: return emptyList()
        return gameState.value.players.filter { it.score == lowestScore }
    }

    fun getPlayerRankings(): List<Pair<Player, Int>> {
        return GameRules.getPlayerRankings(gameState.value.players)
    }
}
