package com.example.nido.game.ai

import com.example.nido.game.GameManager
import com.example.nido.game.rules.GameRules
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.data.model.Hand
import com.example.nido.data.model.Combination



data class AIPlayer(  // data class
    override val id: String,
    override val name: String,
    override val avatar: String,
    override var score: Int = 0,
    override val hand: Hand = Hand() // Provide default values

) : Player {
    override val playerType: PlayerType = PlayerType.AI

    override fun play(gameManager: GameManager): Combination? {
        val possibleMoves: List<Combination> = GameRules.findValidCombinations(hand.cards)

        val playmatCombination = gameManager.gameState.value.currentCombinationOnMat

        return if (playmatCombination != null) {
            possibleMoves.find { GameRules.isValidMove(playmatCombination, it) }
        } else  {
            possibleMoves.minByOrNull { it.value }
        }
    }
}