package com.example.nido.data.model

import com.example.nido.game.GameManager
import com.example.nido.game.rules.GameRules


class AIPlayer(
    override val id: Int,
    override val name: String,
    override val avatar: String,
    override var score: Int = 0,
    override val hand: Hand = Hand()
) : Player {
    override val playerType: PlayerType = PlayerType.AI

    override fun play(gameManager: GameManager): Combination? {
        val possibleMoves: List<Combination> = GameRules.findValidCombinations(hand.cards)

        val playmatCombination = gameManager.gameState.value.currentCombinationOnMat

        return if (playmatCombination != null) {
            possibleMoves.find { GameRules.isValidMove(playmatCombination, it) }
        } else  {
            possibleMoves.minByOrNull { it.value } // AI chooses the lowest valid combination
        }
    }
}