package com.example.nido.game

import com.example.nido.data.model.Card
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.data.repository.DeckRepository
import com.example.nido.data.model.Combination
import com.example.nido.game.rules.GameRules
import com.example.nido.utils.Constants
import com.example.nido.game.multiplayer.NetworkManager
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

object GameManager {
    var players: List<Player> = emptyList()
    var currentTurnIndex: Int = 0
    var deck: MutableList<Card> = mutableListOf()






    var playmat: SnapshotStateList<Card> = mutableStateListOf()
    var discardPile: SnapshotStateList<Card> = mutableStateListOf()



    var pointLimit: Int = Constants.GAME_DEFAULT_POINT_LIMIT

    // ✅ Delegate game-over check to GameRules
    fun isGameOver(): Boolean = GameRules.isGameOver(players, pointLimit)

    // ✅ Delegate ranking calculation to GameRules
    fun getPlayerRankings(): List<Pair<Player, Int>> = GameRules.getPlayerRankings(players)

    // ✅ Delegate winner calculation to GameRules
    fun getGameWinners(): List<Player> = GameRules.getGameWinners(players)

    fun checkRoundEnd(): Boolean {
        val roundWinner = players.firstOrNull { it.hand.isEmpty() }

        if (roundWinner != null) {
            applyRoundScores(roundWinner)
            return true  // ✅ The round ends when someone finishes their hand
        }
        return false  // ✅ The round continues
    }

    private fun applyRoundScores(winner: Player) {
        val losers = players.filter { it.hand.count() > 0 }  // ✅ Uses `count()` instead of `.cards.isNotEmpty()`
        // 🔹 Each loser gets points equal to their remaining card values
        for (loser in losers) {
            loser.score += loser.hand.cards.sumOf { it.value }
        }
    }

    fun startNewGame(selectedPlayers: List<Player>, selectedPointLimit: Int) {
        println("Game started with players: $selectedPlayers and point limit: $selectedPointLimit")
        players = selectedPlayers
        pointLimit = selectedPointLimit
        deck = DeckRepository.generateDeck(shuffle = true)
        dealCards()
        currentTurnIndex = 0
    }

    private fun dealCards() {
        players.forEach { player ->
            repeat(Constants.HAND_SIZE) {
                val card = deck.removeAt(0)
                player.hand.addCard(card)
            }
        }
    }

    fun getCurrentPlayer(): Player = players[currentTurnIndex]

    fun nextTurn() {
        currentTurnIndex = (currentTurnIndex + 1) % players.size
        if (players[currentTurnIndex].playerType == PlayerType.REMOTE) {
            requestRemoteMove(players[currentTurnIndex])
        }
    }

    fun requestRemoteMove(player: Player) {
        // TODO: Implement actual network call
        val move = NetworkManager.receiveMove()
        processMove(player, move)
    }

    fun processMove(player: Player, move: Combination) {
        if (isValidMove(move)) {
            playmat.clear()
            playmat.addAll(move.cards)
            player.hand.removeCombination(move)
            discardPile.addAll(playmat.drop(1)) // Player keeps one card
            nextTurn()
        }
    }

    fun isValidMove(move: Combination): Boolean {
        // TODO: Implement actual game rules
        return true
    }
}
