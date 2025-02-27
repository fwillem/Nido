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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.State  // Correct import

object GameManager {
    // Comment out the old instance variables:
    /*
    var players: List<Player> = emptyList()
    var currentTurnIndex: Int = 0
    var deck: MutableList<Card> = mutableListOf()

    var playmat: SnapshotStateList<Card> = mutableStateListOf()
    var discardPile: SnapshotStateList<Card> = mutableStateListOf()

    var pointLimit: Int = Constants.GAME_DEFAULT_POINT_LIMIT
*/

    // Add ViewModel and GameState properties:
    private val viewModel = GameViewModel()
    private val _gameState: MutableState<GameState> = mutableStateOf(GameState())
    val gameState: State<GameState>
        get() = _gameState.value

    /**
     * ✅ Starts a new game by initializing players and dealing hands.
     */
    fun startNewGame(selectedPlayers: List<Player>, selectedPointLimit: Int) {
        val removedColors = if (selectedPlayers.size <= Constants.GAME_REDUCED_COLOR_THRESHOLD) {
            Constants.REMOVED_COLORS
        } else {
            emptySet()
        }

        val deck = DeckRepository.generateDeck(shuffle = true, removedColors = removedColors)
        val mutableDeck = mutableStateListOf<Card>()
        mutableDeck.addAll(deck)

        // Initialize GameState using the provided values:
        _gameState.value = GameState(
            players = selectedPlayers,
            pointLimit = selectedPointLimit,
            deck = mutableDeck,
            currentPlayerIndex = 0,
            currentCombinationOnMat = null,
            discardPile = mutableStateListOf(),
            screen = GameScreens.PLAYING //  Or SETUP, if applicable
        )

        dealCards() // Call dealCards *after* setting up GameState
    }

    /**
     * ✅ Deals initial hands to players.
     */
    private fun dealCards() {
        // Access players and deck through gameState.value
        val mutableDeck = gameState.deck.toMutableList()
        gameState.players.forEach { player ->
            repeat(Constants.HAND_SIZE) {
                val card = mutableDeck.removeAt(0)
                player.hand.addCard(card)
            }
        }
        val deckSnapshotList = mutableStateListOf<Card>()
        deckSnapshotList.addAll(mutableDeck)
        viewModel.updateGameState(gameState.copy(deck = deckSnapshotList))
    }

    /**
     * ✅ Gets the current player.
     */
    fun getCurrentPlayer(): Player = gameState.players[gameState.currentPlayerIndex]

    /**
     * ✅ Moves to the next player's turn.
     */
    fun nextTurn() {
        val nextIndex = (gameState.currentPlayerIndex + 1) % gameState.numberOfPlayers
        viewModel.updateGameState(gameState.copy(currentPlayerIndex = nextIndex))
        val nextPlayer = gameState.players[gameState.currentPlayerIndex]

        if (nextPlayer.playerType == PlayerType.AI) {
            handleAIMove(nextPlayer)
        }
    }

    /**
     * ✅ Plays a combination of selected cards.
     */
    fun playCombination(selectedCards: List<Card>) {
        if (selectedCards.isEmpty()) {
            println("❌ playCombination: No cards selected")
            return
        }

        val currentCombination = gameState.currentCombinationOnMat ?: Combination(mutableListOf())
        val newCombination = Combination(selectedCards.toMutableList())

        if (GameRules.isValidMove(currentCombination, newCombination)) {
            println("✅ Valid combination played: $selectedCards")

            // ✅ Log playmat before update
            println("🔹 Before Update: Playmat = ${gameState.currentCombinationOnMat?.cards?.joinToString { "${it.value} ${it.color}" }}")

            // ✅ Update playmat and player hand
            val updatedPlayer = getCurrentPlayer().copy() // Create a copy
            updatedPlayer.hand.removeCombination(newCombination)
            val updatedPlayers = gameState.players.toMutableList()
            updatedPlayers[gameState.currentPlayerIndex] = updatedPlayer

            //Pick a card
            val newDiscardPile: SnapshotStateList<Card>
            var cardToKeep : Card? = null
            if (currentCombination.cards.isNotEmpty()) {
                println("🔹 Pick one card to keep from: $currentCombination")
                // TODO: Implement logic for choosing one card. For now take first
                cardToKeep = currentCombination.cards.firstOrNull()

                newDiscardPile = mutableStateListOf()
                newDiscardPile.addAll(gameState.discardPile)
                newDiscardPile.addAll(currentCombination.cards.subList(1,currentCombination.cards.size))
            }
            else{
                println("🔹 No cards to keep from playmat since it's empty")
                newDiscardPile = gameState.discardPile
            }

            //gameState = gameState.copy( //Remove
            viewModel.updateGameState(gameState.copy( //Update through ViewModel
                players = updatedPlayers,
                currentCombinationOnMat = newCombination,
                discardPile = newDiscardPile
            ))
            cardToKeep?.let{getCurrentPlayer().hand.addCard(it)} //Add the card to player hand at the end of the turn

            // ✅ Log playmat after update
            println("🔹 After Update: Playmat = ${gameState.currentCombinationOnMat?.cards?.joinToString { "${it.value} ${it.color}" }}")


            nextTurn()  // ✅ Change turn
        } else {
            println("❌ Invalid combination! Move rejected.")
        }
    }

    fun processAIMove() {
        val currentPlayer = getCurrentPlayer()
        if (currentPlayer.playerType == PlayerType.AI) {
            handleAIMove(currentPlayer)
        } else {
            println("❌ ERROR: Not AI's turn!")
        }
    }

    /**
     * ✅ Handles AI move.
     */
    private fun handleAIMove(aiPlayer: Player) {
        val bestMove = aiPlayer.play(GameContext) //TODO GameContext
        bestMove?.let { processMove(aiPlayer, it.cards) }
    }

    /**
     * ✅ Checks if the move is valid based on game rules.
     */
    fun isValidMove(selectedCards: List<Card>): Boolean {
        if (selectedCards.isEmpty()) {
            println("❌ IsValidMove: No cards selected")
            return false
        }

        println("✅ IsValidMove: Selected Cards = ${selectedCards.joinToString { "${it.value} ${it.color}" }}")

        val currentCombination = gameState.currentCombinationOnMat ?: Combination(mutableListOf())


        println("✅ IsValidMove: Current Combination = ${currentCombination.cards.joinToString { "${it.value} ${it.color}" }}")

        val selectedCombination = Combination(selectedCards.toMutableList())
        println("✅ IsValidMove: Selected Combination = ${selectedCombination.cards.joinToString { "${it.value} ${it.color}" }}")

        val isValid = GameRules.isValidMove(currentCombination, selectedCombination)

        println("✅ IsValidMove: Move validation result = $isValid")

        return isValid
    }

    /**
     * ✅ Processes a move if valid, clears playmat & asks player to pick a card.
     */
    fun processMove(player: Player, selectedCards: List<Card>) {
        if (!isValidMove(selectedCards)) {
            println("❌ Invalid move: ${selectedCards.joinToString()}")
            return
        }

        // Update playmat
        val newPlaymat = mutableStateListOf<Card>()
        newPlaymat.addAll(selectedCards)


        // Remove played cards from hand
        val updatedPlayer = player.copy() // Create a copy of the player
        selectedCards.forEach { updatedPlayer.hand.removeCard(it) }
        val updatedPlayers = gameState.players.toMutableList()
        updatedPlayers[gameState.currentPlayerIndex] = updatedPlayer // Put the copy back in the list

        // If playmat was not empty, ask player to pick a card before ending turn, we add it in current player hand
        //In this first implementation we always take first card
        val newDiscardPile = mutableStateListOf<Card>()
        newDiscardPile.addAll(gameState.discardPile)
        gameState.currentCombinationOnMat?.cards?.let {
            if (it.isNotEmpty()) {
                val cardToKeep = it.firstOrNull()
                cardToKeep?.let { updatedPlayer.hand.addCard(it) }
                newDiscardPile.addAll(it.subList(1, it.size))
            }
        }

        // Update game state using copy
        //gameState = gameState.copy(//Remove
        viewModel.updateGameState(gameState.copy( //Update through ViewModel
            players = updatedPlayers,
            currentCombinationOnMat = Combination(newPlaymat),
            discardPile = newDiscardPile
        ))

        // Check if round ends
        if (checkRoundEnd()) return

        // Move to next turn
        nextTurn()
    }

    /**
     * ✅ Checks if a round has ended (i.e., a player emptied their hand).
     */
    fun checkRoundEnd(): Boolean {
        val roundWinner = gameState.players.firstOrNull { it.hand.isEmpty() }

        if (roundWinner != null) {
            applyRoundScores(roundWinner)
            return true
        }
        return false
    }

    /**
     * ✅ Updates scores after a round ends.
     */
    private fun applyRoundScores(winner: Player) {
        val updatedPlayers = gameState.players.map { player ->
            if (player.hand.cards.isNotEmpty()) {
                val newScore = player.score + player.hand.cards.sumOf { it.value }
                player.copy(score = newScore) // Update score using copy
            } else {
                player // Return winner unchanged
            }
        }
        //gameState = gameState.copy(players = updatedPlayers) // Update players using copy //Remove
        viewModel.updateGameState(gameState.copy(players = updatedPlayers))  //Update through ViewModel

    }

    /**
     * ✅ Checks if the game is over (if a player reaches the point limit).
     */
    fun isGameOver(): Boolean = gameState.players.any { it.score >= gameState.pointLimit }

    /**
     * ✅ Gets the overall game winners (lowest score).
     */
    fun getGameWinners(): List<Player> {
        val lowestScore = gameState.players.minOfOrNull { it.score } ?: return emptyList()
        return gameState.players.filter { it.score == lowestScore }
    }

    /**
     * ✅ Gets player rankings based on score.
     */
    fun getPlayerRankings(): List<Pair<Player, Int>> = GameRules.getPlayerRankings(gameState.players)

    fun resetPlaymat() {
        val newDiscardPile = mutableStateListOf<Card>()
        newDiscardPile.addAll(gameState.discardPile)
        gameState.currentCombinationOnMat?.let {
            newDiscardPile.addAll(it.cards)
        }
        viewModel.updateGameState(gameState.copy(currentCombinationOnMat = null, discardPile = newDiscardPile))
    }
}