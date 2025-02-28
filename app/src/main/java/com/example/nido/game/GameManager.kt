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
        val mutableDeck = mutableStateListOf<Card>()
        mutableDeck.addAll(deck)

        println("🃏 Deck Size Before Dealing: ${mutableDeck.size}")  // ✅ Debugging line


        viewModel.updateGameState(GameState(
            players = selectedPlayers,
            pointLimit = selectedPointLimit,
            deck = mutableDeck,
            currentPlayerIndex = 0,
            currentCombinationOnMat = null,
            discardPile = mutableStateListOf(),
            screen = GameScreens.PLAYING
        ))

        dealCards(selectedPlayers)  // ✅ Pass correct player list
    }

    private fun dealCards(selectedPlayers: List<Player>) {
        val mutableDeck = gameState.value.deck.toMutableList()
        val mutablePlayers = selectedPlayers.toMutableList()

        if (mutableDeck.isEmpty()) {
            throw IllegalStateException("❌ Deck is empty before dealing cards! This should never happen.")
        }

        mutablePlayers.forEachIndexed { index, player ->
            val updatedHand = player.hand.copy()  // Deep copy of Hand

            repeat(Constants.HAND_SIZE) {
                if (mutableDeck.isNotEmpty()) {  // ✅ Check before removing a card
                    val card = mutableDeck.removeFirst()  // ✅ SAFER WAY: removeFirst()
                    updatedHand.addCard(card)
                } else {
                    throw IllegalStateException("❌ Not enough cards in the deck to deal hands!")
                }
            }

            mutablePlayers[index] = when (player) {
                is LocalPlayer -> player.copy(hand = updatedHand)
                is AIPlayer -> player.copy(hand = updatedHand)
                else -> throw IllegalArgumentException("Unknown player type")
            }
        }

        val deckSnapshotList = mutableStateListOf<Card>().apply { addAll(mutableDeck) }

        viewModel.updateGameState(gameState.value.copy(deck = deckSnapshotList, players = mutablePlayers))
    }


    fun getCurrentPlayer(): Player = gameState.value.players[gameState.value.currentPlayerIndex]

    fun nextTurn() {
        val nextIndex = (gameState.value.currentPlayerIndex + 1) % gameState.value.numberOfPlayers
        viewModel.updateGameState(gameState.value.copy(currentPlayerIndex = nextIndex))

        val nextPlayer = gameState.value.players[gameState.value.currentPlayerIndex]
        if (nextPlayer.playerType == PlayerType.AI) {
            handleAIMove(nextPlayer)
        }
    }

    fun playCombination(selectedCards: List<Card>) {
        if (selectedCards.isEmpty()) {
            println("playCombination: No cards selected")
            return
        }

        val currentCombination = gameState.value.currentCombinationOnMat ?: Combination(mutableListOf())
        val newCombination = Combination(selectedCards.toMutableList())

        if (GameRules.isValidMove(currentCombination, newCombination)) {
            println("Valid combination played: $selectedCards")
            println("Before Update: Playmat = ${gameState.value.currentCombinationOnMat?.cards?.joinToString { "${it.value} ${it.color}" }}")

            val updatedHand = getCurrentPlayer().hand.copy() // Deep copy of Hand
            updatedHand.removeCombination(newCombination)
            val updatedPlayers = gameState.value.players.toMutableList()
            val currentPlayer = getCurrentPlayer()
            updatedPlayers[gameState.value.currentPlayerIndex] = when (currentPlayer){ // Use when for type checking
                is LocalPlayer -> currentPlayer.copy(hand = updatedHand)
                is AIPlayer -> currentPlayer.copy(hand = updatedHand)
                else -> throw IllegalArgumentException("Unknown player type")
            }

            var cardToKeep : Card? = null
            val newDiscardPile: SnapshotStateList<Card> = if (currentCombination.cards.isNotEmpty()) {
                println("Pick one card to keep from: $currentCombination")
                cardToKeep = currentCombination.cards.firstOrNull() // TODO: Implement choice logic
                mutableStateListOf<Card>().also {
                    it.addAll(gameState.value.discardPile)
                    it.addAll(currentCombination.cards.subList(1,currentCombination.cards.size))
                }
            } else{
                println("No cards to keep from playmat since it's empty")
                gameState.value.discardPile
            }

            viewModel.updateGameState(gameState.value.copy(
                players = updatedPlayers,
                currentCombinationOnMat = newCombination,
                discardPile = newDiscardPile
            ))

            val updatedHandAfterPick = getCurrentPlayer().hand.copy()
            cardToKeep?.let{updatedHandAfterPick.addCard(it)}
            val index = gameState.value.currentPlayerIndex
            updatedPlayers[index] = when (val p = updatedPlayers[index]){ // Use when for type checking
                is LocalPlayer -> p.copy(hand = updatedHandAfterPick)
                is AIPlayer -> p.copy(hand = updatedHandAfterPick)
                else -> throw IllegalArgumentException("Unknown player type")
            }
            viewModel.updateGameState(gameState.value.copy(players = updatedPlayers))
            println("After Update: Playmat = ${gameState.value.currentCombinationOnMat?.cards?.joinToString { "${it.value} ${it.color}" }}")

            nextTurn()
        } else {
            println("Invalid combination! Move rejected.")
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
        val bestMove = aiPlayer.play(this) // Pass GameManager instance
        bestMove?.let { processMove(it.cards) }
    }

    fun isValidMove(selectedCards: List<Card>): Boolean {
        if (selectedCards.isEmpty()) {
            println("IsValidMove: No cards selected")
            return false
        }

        println("IsValidMove: Selected Cards = ${selectedCards.joinToString { "${it.value} ${it.color}" }}")

        val currentCombination = gameState.value.currentCombinationOnMat ?: Combination(mutableListOf())

        println("IsValidMove: Current Combination = ${currentCombination.cards?.joinToString { "${it.value} ${it.color}" }}")

        val selectedCombination = Combination(selectedCards.toMutableList())
        println("IsValidMove: Selected Combination = ${selectedCombination.cards?.joinToString { "${it.value} ${it.color}" }}")

        return GameRules.isValidMove(currentCombination, selectedCombination)
    }

    fun processMove(selectedCards: List<Card>) {
        if (!isValidMove(selectedCards)) {
            println("Invalid move: ${selectedCards.joinToString()}")
            return
        }

        // Update playmat
        val newPlaymat = mutableStateListOf<Card>()
        newPlaymat.addAll(selectedCards)

        // Remove played cards from hand
        val updatedHand = getCurrentPlayer().hand.copy() // Deep copy of Hand
        selectedCards.forEach { updatedHand.removeCard(it) }

        val updatedPlayers = gameState.value.players.toMutableList()
        val currentPlayer = getCurrentPlayer()
        updatedPlayers[gameState.value.currentPlayerIndex] = when (currentPlayer){ // Use when for type checking
            is LocalPlayer -> currentPlayer.copy(hand = updatedHand)
            is AIPlayer -> currentPlayer.copy(hand = updatedHand)
            else -> throw IllegalArgumentException("Unknown player type")
        }

        // If playmat was not empty, ask player to pick a card before ending turn, we add it in current player hand
        //In this first implementation we always take first card
        val newDiscardPile = mutableStateListOf<Card>()
        newDiscardPile.addAll(gameState.value.discardPile)
        gameState.value.currentCombinationOnMat?.cards?.let{
            if (it.isNotEmpty()) {
                val cardToKeep = it.firstOrNull()
                val updatedHandAfterPick = getCurrentPlayer().hand.copy()
                cardToKeep?.let { updatedHandAfterPick.addCard(it) }
                val index = gameState.value.currentPlayerIndex
                updatedPlayers[index] = when (val p = updatedPlayers[index]){ // Use when for type checking
                    is LocalPlayer -> p.copy(hand = updatedHandAfterPick)
                    is AIPlayer -> p.copy(hand = updatedHandAfterPick)
                    else -> throw IllegalArgumentException("Unknown player type")
                }
                newDiscardPile.addAll(it.subList(1, it.size))
            }
        }

        viewModel.updateGameState(gameState.value.copy(
            players = updatedPlayers,
            currentCombinationOnMat = Combination(newPlaymat),
            discardPile = newDiscardPile
        ))

        if (checkRoundEnd()) return

        nextTurn()
    }

    fun checkRoundEnd(): Boolean {
        val roundWinner = gameState.value.players.firstOrNull { it.hand.isEmpty() }

        if (roundWinner != null) {
            applyRoundScores(roundWinner)
            return true
        }
        return false
    }

    private fun applyRoundScores(winner: Player) {
        val updatedPlayers = gameState.value.players.map { player ->
            if (player.hand.cards.isNotEmpty()) {
                val newScore = player.score + player.hand.cards.sumOf { it.value }
                when (player) { // Use when for type checking
                    is LocalPlayer -> player.copy(score = newScore)
                    is AIPlayer -> player.copy(score = newScore)
                    else -> throw IllegalArgumentException("Unknown player type")
                }
            } else {
                when (player) {
                    is LocalPlayer -> player.copy() //Still using copy
                    is AIPlayer -> player.copy() //Still using copy
                    else -> throw IllegalArgumentException("Unknown player type")
                }
            }
        }
        viewModel.updateGameState(gameState.value.copy(players = updatedPlayers))
    }

    fun isGameOver(): Boolean = gameState.value.players.any { it.score >= gameState.value.pointLimit }

    fun getGameWinners(): List<Player> {
        val lowestScore = gameState.value.players.minOfOrNull { it.score } ?: return emptyList()
        return gameState.value.players.filter { it.score == lowestScore }
    }

    fun getPlayerRankings(): List<Pair<Player, Int>> = GameRules.getPlayerRankings(gameState.value.players)

    fun resetPlaymat() {
        val newDiscardPile = mutableStateListOf<Card>()
        newDiscardPile.addAll(gameState.value.discardPile)
        gameState.value.currentCombinationOnMat?.let {
            newDiscardPile.addAll(it.cards)
        }
        viewModel.updateGameState(gameState.value.copy(currentCombinationOnMat = null, discardPile = newDiscardPile))
    }
}