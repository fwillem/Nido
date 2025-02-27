package com.example.nido.game

//import androidx.compose.runtime.MutableState // Remove this for now
import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.data.repository.DeckRepository
import com.example.nido.game.rules.GameRules
import com.example.nido.utils.Constants
import androidx.compose.runtime.mutableStateListOf
//import androidx.compose.runtime.mutableStateOf // Remove this for now
import androidx.compose.runtime.snapshots.SnapshotStateList
//import androidx.compose.runtime.State  // Keep this import, but we won't use it *yet*

class GameManager {

    // Keep your existing properties for now
    var players: List<Player> = emptyList()
    var currentTurnIndex: Int = 0
    var deck: MutableList<Card> = mutableListOf() // Add deck

    var playmat: SnapshotStateList<Card> = mutableStateListOf()
    var discardPile: SnapshotStateList<Card> = mutableStateListOf()

    var pointLimit: Int = Constants.GAME_DEFAULT_POINT_LIMIT
    //Keep constructor
    /*
    private val _gameState: MutableState<GameState> = mutableStateOf(GameState())
    val gameState: State<GameState> = _gameState
*/

    /**
     * ✅ Starts a new game by initializing players and dealing hands.
     */
    fun initGame(numPlayers: Int, maxPoints: Int = Constants.GAME_DEFAULT_POINT_LIMIT) {
        if (numPlayers !in Constants.GAME_MIN_PLAYERS..Constants.GAME_MAX_PLAYERS) {
            throw IllegalArgumentException("Invalid number of players")
        }

        val removedColors = if (numPlayers <= Constants.GAME_REDUCED_COLOR_THRESHOLD) {
            Constants.REMOVED_COLORS
        } else {
            emptySet()
        }


        // Use DeckRepository to create and shuffle the deck
        deck = DeckRepository.generateDeck(shuffle = true, removedColors = removedColors)


        var players: List<Player> = emptyList()

        dealCards() // Deal the cards

        //No more gamestate for now
        /* _gameState.value = GameState(
             numberOfPlayers = numPlayers,
             pointLimit = maxPoints,  // Corrected name
             players = players,
             currentPlayerIndex = 0,
             currentCombinationOnMat = null,
             discardPile = mutableStateListOf(),
             screen = GameScreens.PLAYING, // Or SETUP
             soundOn = true,
             showConfirmExitDialog = false
         )*/
        pointLimit = maxPoints
        currentTurnIndex = 0
        playmat = mutableStateListOf()
        discardPile = mutableStateListOf()
    }

    /**
     * ✅ Deals initial hands to players.
     */
    //Use deck variable
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
    //Use players variable
    fun getCurrentPlayer(): Player = players[currentTurnIndex]

    /**
     * ✅ Moves to the next player's turn.
     */
    //Use players and currentTurnIndex variables
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
    //Use players, currentTurnIndex, playmat variables
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
    //Use playmat variable
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
    //Use playmat, discardPile and players variables
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
    //use players variable
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
    //use players variable
    private fun applyRoundScores(winner: Player) {
        val losers = players.filter { it.hand.count() > 0 }

        for (loser in losers) {
            loser.score += loser.hand.cards.sumOf { it.value }
        }
    }

    /**
     * ✅ Checks if the game is over (if a player reaches the point limit).
     */
    //Use players and pointLimit
    fun isGameOver(): Boolean = players.any { it.score >= pointLimit }

    /**
     * ✅ Gets the overall game winners (lowest score).
     */
    //use players variable
    fun getGameWinners(): List<Player> {
        val lowestScore = players.minOfOrNull { it.score } ?: return emptyList()
        return players.filter { it.score == lowestScore }
    }

    /**
     * ✅ Gets player rankings based on score.
     */
    //Use players variable
    fun getPlayerRankings(): List<Pair<Player, Int>> = GameRules.getPlayerRankings(players)
}