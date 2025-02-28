package com.example.nido.game

import androidx.compose.runtime.MutableState
import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.data.repository.DeckRepository
import com.example.nido.game.rules.GameRules
import com.example.nido.utils.Constants
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.example.nido.game.ai.AIPlayer

object GameManager {
    private val viewModel = GameViewModel()
    private val _gameState: MutableState<GameState> = mutableStateOf(GameState())
    val gameState: State<GameState>
        get() = _gameState

    fun startNewGame(selectedPlayers: List<Player>, selectedPointLimit: Int) {
        val removedColors = if (selectedPlayers.size <= Constants.GAME_REDUCED_COLOR_THRESHOLD) {
            Constants.REMOVED_COLORS
        } else {
            emptySet()
        }

        val deck = DeckRepository.generateDeck(shuffle = true, removedColors = removedColors)
        val mutableDeck = mutableStateListOf<Card>().apply { addAll(deck) }

        val newGameState = GameState(
            players = selectedPlayers.toMutableList(),  // ✅ Ensure mutability
            pointLimit = selectedPointLimit,
            deck = mutableDeck,
            currentPlayerIndex = 0,
            currentCombinationOnMat = null,
            discardPile = mutableStateListOf(),
            screen = GameScreens.PLAYING
        )

        dealCards(newGameState)  // ✅ Modify local state first

        viewModel.updateGameState(newGameState)  // ✅ Save final state after dealing cards
    }

    private fun dealCards(state: GameState) {
        val mutableDeck = state.deck.toMutableList()
        val mutablePlayers = state.players.toMutableList()

        mutablePlayers.forEachIndexed { index, player ->
            val updatedHand = player.hand.copy()
            repeat(Constants.HAND_SIZE) {
                if (mutableDeck.isNotEmpty()) {
                    val card = mutableDeck.removeAt(0)
                    updatedHand.addCard(card)
                } else {
                    throw IllegalStateException("Deck is empty before dealing all cards!")
                }
            }
            mutablePlayers[index] = when (player) {
                is LocalPlayer -> player.copy(hand = updatedHand)
                is AIPlayer -> player.copy(hand = updatedHand)
                else -> throw IllegalArgumentException("Unknown player type")
            }
        }

        // ✅ Instead of reassigning 'val', update existing mutable lists
        state.deck.clear()
        state.deck.addAll(mutableDeck)
        state.players.clear()
        state.players.addAll(mutablePlayers)
    }

    private fun getCurrentPlayer(): Player = gameState.value.players[gameState.value.currentPlayerIndex]

    fun playCombination(selectedCards: List<Card>) {
        if (selectedCards.isEmpty()) {
            println("playCombination: No cards selected")
            return
        }

        val currentGameState = gameState.value
        val currentCombination = currentGameState.currentCombinationOnMat ?: Combination(mutableListOf())
        val newCombination = Combination(selectedCards.toMutableList())

        if (!GameRules.isValidMove(currentCombination, newCombination)) {
            println("Invalid combination! Move rejected.")
            return
        }

        val updatedHand = getCurrentPlayer().hand.copy().apply { removeCombination(newCombination) }
        val updatedPlayers = currentGameState.players.toMutableList().apply {
            this[currentGameState.currentPlayerIndex] = when (val currentPlayer = getCurrentPlayer()) {
                is LocalPlayer -> currentPlayer.copy(hand = updatedHand)
                is AIPlayer -> currentPlayer.copy(hand = updatedHand)
                else -> throw IllegalArgumentException("Unknown player type")
            }
        }

        var cardToKeep: Card? = null
        val newDiscardPile = mutableStateListOf<Card>().apply {
            addAll(currentGameState.discardPile)
            if (currentCombination.cards.isNotEmpty()) {
                cardToKeep = currentCombination.cards.firstOrNull()
                addAll(currentCombination.cards.drop(1))
            }
        }

        cardToKeep?.let { updatedHand.addCard(it) }

        val updatedState = currentGameState.copy(
            players = updatedPlayers,
            currentCombinationOnMat = newCombination,
            discardPile = newDiscardPile
        )

        viewModel.updateGameState(updatedState)

        nextTurn()  // ✅ Internally controlled turn progression
    }

    private fun nextTurn() {
        val currentGameState = gameState.value
        val nextIndex = (currentGameState.currentPlayerIndex + 1) % currentGameState.players.size

        val updatedState = currentGameState.copy(currentPlayerIndex = nextIndex)
        viewModel.updateGameState(updatedState)

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
            println("ERROR: Not AI's turn!")
        }
    }

    private fun handleAIMove(aiPlayer: Player) {
        val bestMove = aiPlayer.play(this)
        bestMove?.let { processMove(it.cards) }
    }

    fun isValidMove(selectedCards: List<Card>): Boolean {
        val currentCombination = gameState.value.currentCombinationOnMat ?: Combination(mutableListOf())
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
            this[currentGameState.currentPlayerIndex] = when (val currentPlayer = getCurrentPlayer()) {
                is LocalPlayer -> currentPlayer.copy(hand = updatedHand)
                is AIPlayer -> currentPlayer.copy(hand = updatedHand)
                else -> throw IllegalArgumentException("Unknown player type")
            }
        }

        val updatedState = currentGameState.copy(
            players = updatedPlayers,
            currentCombinationOnMat = Combination(newPlaymat)
        )

        viewModel.updateGameState(updatedState)
    }

    private fun checkRoundEnd(): Boolean {
        return gameState.value.players.any { it.hand.isEmpty() }
    }

    fun isGameOver(): Boolean = gameState.value.players.any { it.score >= gameState.value.pointLimit }

    fun getGameWinners(): List<Player> {
        val lowestScore = gameState.value.players.minOfOrNull { it.score } ?: return emptyList()
        return gameState.value.players.filter { it.score == lowestScore }
    }
}
