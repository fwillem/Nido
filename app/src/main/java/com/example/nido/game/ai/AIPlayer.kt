package com.example.nido.game.ai

import com.example.nido.game.GameManager
import com.example.nido.game.rules.GameRules
import com.example.nido.data.model.*

class AIPlayer(
    override val id: String,
    override val name: String,
    override val avatar: String,
    override var score: Int = 0,
    override val hand: Hand = Hand()
) : Player {
    override val playerType: PlayerType = PlayerType.AI

    override fun play(gameManager: GameManager): Combination? {
        val possibleMoves: List<Combination> = GameRules.findValidCombinations(hand.cards)
        val playmatCombination = gameManager.gameState.value.currentCombinationOnMat

        return possibleMoves.find { GameRules.isValidMove(playmatCombination, it) }

    }

    override fun copy(
        id: String,
        name: String,
        avatar: String,
        score: Int,
        hand: Hand
    ): Player {
        return AIPlayer(id, name, avatar, score, hand)
    }

    override fun toString(): String {
        return "AIPlayer(id='$id', name='$name', avatar='$avatar', playerType=$playerType, score=$score, hand=$hand)"
    }
}
