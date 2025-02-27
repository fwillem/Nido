package com.example.nido.game

import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.data.repository.DeckRepository
import com.example.nido.game.rules.GameRules
import com.example.nido.utils.Constants
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

object GameManager { // Correct: GameManager is an object
    var players: List<Player> = emptyList()
    var currentTurnIndex: Int = 0
    var deck: MutableList<Card> = mutableListOf() // Add deck

    var playmat: SnapshotStateList<Card> = mutableStateListOf()
    var discardPile: SnapshotStateList<Card> = mutableStateListOf()

    var pointLimit: Int = Constants.GAME_DEFAULT_POINT_LIMIT

    /**
     * ✅ Starts a new game by initializing players and dealing hands.
     */
    fun startNewGame(selectedPlayers: List<Player>, selectedPointLimit: Int) {
        players = selectedPlayers
        pointLimit = selectedPointLimit

        val removedColors = if (players.size <= Constants.GAME_REDUCED_COLOR_THRESHOLD) {
            Constants.REMOVED_COLORS
        } else {
            emptySet()
        }

        deck = DeckRepository.generateDeck(shuffle = true, removedColors = removedColors)
        dealCards()
        currentTurnIndex = 0
    }

    /**
     * ✅ Deals initial hands to players.
     */
    private fun dealCards() {
        players.forEach { player ->
            repeat(Constants.HAND_SIZE) {
                val card = deck.removeAt(0)
                player.hand.addCard(card)
            }
        }
    }

    /**
     * ✅ Gets the current player.
     */
    fun getCurrentPlayer(): Player = players[currentTurnIndex]

    /**
     * ✅ Moves to the next player's turn.
     */
    fun nextTurn() {
        currentTurnIndex = (currentTurnIndex + 1) % players.size
        val nextPlayer = players[currentTurnIndex]

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

        val currentCombination = if (playmat.isEmpty()) Combination(mutableListOf()) else Combination(playmat.toMutableList())
        val newCombination = Combination(selectedCards.toMutableList())

        if (GameRules.isValidMove(currentCombination, newCombination)) {
            println("✅ Valid combination played: $selectedCards")

            // ✅ Log playmat before update
            println("🔹 Before Update: Playmat = ${playmat.joinToString { "${it.value} ${it.color}" }}")

            // ✅ Clear and update playmat
            playmat.clear()
            playmat.addAll(newCombination.cards)

            // ✅ Log playmat after update
            println("🔹 After Update: Playmat = ${playmat.joinToString { "${it.value} ${it.color}" }}")

            val currentPlayer = getCurrentPlayer()
            currentPlayer.hand.removeCombination(newCombination)  // ✅ Remove from hand

            // Ask player to pick one card from the combination (except first round)
            if (playmat.isNotEmpty()) {
                println("🔹 Pick one card to keep from: $playmat")
                // TODO: Implement logic for choosing one card
            } else {
                println("🔹 No cards to keep from playmat since it's empty")
            }

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

        val currentCombination = if (playmat.isEmpty()) {
            println("⚠️ IsValidMove: Playmat is empty, setting initial combination.")
            Combination(mutableListOf()) // ✅ Safe empty combination
        } else {
            Combination(playmat.toMutableList()) // ✅ Properly constructed combination
        }

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

        // Move cards to playmat
        playmat.clear()
        playmat.addAll(selectedCards)

        // Remove played cards from hand
        selectedCards.forEach { player.hand.removeCard(it) }

        // If playmat was not empty, ask player to pick a card before ending turn
        if (discardPile.isNotEmpty()) {
            val cardToKeep = player.hand.cards.firstOrNull()
            if (cardToKeep != null) {
                player.hand.addCard(cardToKeep)
                discardPile.remove(cardToKeep)
            }
        }

        // Check if round ends
        if (checkRoundEnd()) return

        // Move to next turn
        nextTurn()
    }

    /**
     * ✅ Checks if a round has ended (i.e., a player emptied their hand).
     */
    fun checkRoundEnd(): Boolean {
        val roundWinner = players.firstOrNull { it.hand.isEmpty() }

        if (roundWinner != null) {
            applyRoundScores(roundWinner)
            return true  // ✅ Round ends immediately
        }
        return false
    }

    /**
     * ✅ Updates scores after a round ends.
     */
    private fun applyRoundScores(winner: Player) {
        val losers = players.filter { it.hand.count() > 0 }

        for (loser in losers) {
            loser.score += loser.hand.cards.sumOf { it.value }
        }
    }

    /**
     * ✅ Checks if the game is over (if a player reaches the point limit).
     */
    fun isGameOver(): Boolean = players.any { it.score >= pointLimit }

    /**
     * ✅ Gets the overall game winners (lowest score).
     */
    fun getGameWinners(): List<Player> {
        val lowestScore = players.minOfOrNull { it.score } ?: return emptyList()
        return players.filter { it.score == lowestScore }
    }

    /**
     * ✅ Gets player rankings based on score.
     */
    fun getPlayerRankings(): List<Pair<Player, Int>> = GameRules.getPlayerRankings(players)
}