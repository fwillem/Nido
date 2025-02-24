package com.example.nido.game

import com.example.nido.data.model.Card
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType

import com.example.nido.data.repository.DeckRepository
import com.example.nido.game.rules.GameSettings
import com.example.nido.multiplayer.NetworkManager
import com.example.nido.data.model.Combination
import com.example.nido.utils.Constants


object GameManager {
    var players: List<Player> = emptyList()
    var currentTurnIndex: Int = 0
    var deck: MutableList<Card> = mutableListOf()
    var playmat: MutableList<Card> = mutableListOf()
    var discardPile: MutableList<Card> = mutableListOf()
    var pointLimit: Int = GameSettings.gamePointLimit

    fun startNewGame(selectedPlayers: List<Player>, selectedPointLimit: Int) {
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
