package com.example.nido.game.ai

import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.game.GameContext
import com.example.nido.game.rules.GameRules

class AIPlayer(id: String, name: String, avatar: String) : Player(id, name, avatar, PlayerType.AI) {
    override fun play(gameContext: GameContext): Combination? {
        val possibleMoves: List<Combination> = GameRules.findValidCombinations(hand.cards)

        val playmatCombination = gameContext.getCurrentPlaymatCombination()

        if (playmatCombination != null) {
            return possibleMoves.find { it.value > playmatCombination.value }
        } else  {
            return possibleMoves.minByOrNull { it.value } // AI chooses the lowest valid combination
        }

    }
}
