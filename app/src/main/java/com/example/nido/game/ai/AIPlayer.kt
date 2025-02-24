package com.example.nido.game.ai

import com.example.nido.data.model.Combination


class AIPlayer {
    fun chooseMove(validMoves: List<Combination>): Combination {
        return validMoves.minByOrNull { it.value } ?: validMoves.first()
    }
}
