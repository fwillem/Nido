package com.example.nido.game

import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.data.repository.DeckRepository

object GameContext {
    var discardPile: MutableList<Card> = mutableListOf()
    var deck: MutableList<Card> = DeckRepository.generateDeck(shuffle = true)
    var previousMoves: MutableList<Pair<Player, Combination>> = mutableListOf()
    var moveDurations: MutableMap<Player, MutableList<Long>> = mutableMapOf()
    var skipCounts: MutableMap<Player, Int> = mutableMapOf()
    var estimatedHands: MutableMap<Player, List<Card>> = mutableMapOf()

    fun getCurrentPlaymatCombination(): Combination? {
        return if (playmat.isNotEmpty()) Combination(playmat) else null
    }

    fun recordMove(player: Player, combination: Combination, duration: Long) {
        previousMoves.add(player to combination)
        moveDurations.getOrPut(player) { mutableListOf() }.add(duration)
    }

    fun recordSkip(player: Player) {
        skipCounts[player] = skipCounts.getOrDefault(player, 0) + 1
    }
}
