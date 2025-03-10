package com.example.nido.game.rules

import com.example.nido.data.model.Card
import com.example.nido.data.model.CardColor
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.example.nido.utils.Constants
import com.example.nido.data.model.Hand


object GameRules {

    fun isValidCombination(combination: Combination): Boolean {
        // A valid combination is either cards of the same color of cards of the same value
        if (combination.cards.isEmpty()) {
            return false
        }

        val firstCard = combination.cards.first()
        val allSameColor = combination.cards.all { it.color == firstCard.color }
        val allSameValue = combination.cards.all { it.value == firstCard.value }

        return allSameColor || allSameValue
    }

    fun isValidMove(current: Combination, newMove: Combination): Boolean {
        return try {
            //  Prevent empty combination crashes
            if (newMove.cards.isEmpty()) {
                TRACE(VERBOSE) { "New move has no cards!" }
                return false
            }

            // First we need to test that the given combination is valid
            if (!isValidCombination(newMove)) {
                TRACE(VERBOSE) { "New move is not valid!" }
                return false
            }

            // Safe comparison
            val isValid = (newMove.value > current.value) && (newMove.cards.size <= current.cards.size + 1)

            TRACE(VERBOSE) { "isValidMove: Current = ${current.value}, New = ${newMove.value}, Card Size = ${newMove.cards.size}, Current Size = ${current.cards.size}, Allowed = $isValid" }
            isValid

        } catch (e: Exception) {
            TRACE(FATAL) { "❌❌❌❌ FATAL ERROR in isValidMove: ${e.message}" }
            false  // ✅ Fail gracefully instead of crashing
        }
    }


    fun findValidCombinations(cards: List<Card>): List<Combination> {
        val validCombinations = mutableListOf<Combination>()

        val colorGroups = cards.groupBy { it.color }
            .mapValues { it.value.sortedByDescending { card -> card.value } }

        val valueGroups = cards.groupBy { it.value }
            .mapValues { it.value.sortedByDescending { card -> card.color.ordinal } }

        val mainCombinations = mutableListOf<Combination>()

        colorGroups.values.forEach { group ->
            if (group.size >= 2) {
                mainCombinations.add(Combination(group.toMutableList()))
            }
        }
        valueGroups.values.forEach { group ->
            if (group.size >= 2) {
                mainCombinations.add(Combination(group.toMutableList()))
            }
        }

        validCombinations.addAll(mainCombinations)

        for (combination in mainCombinations) {
            val subsetCombinations = generateAllSubcombinations(combination.cards)
            validCombinations.addAll(subsetCombinations)
        }

        for (group in colorGroups.values) {
            if (group.size >= 2) {
                validCombinations.addAll(generateAllSubcombinations(group))
            }
        }
        for (group in valueGroups.values) {
            if (group.size >= 2) {
                validCombinations.addAll(generateAllSubcombinations(group))
            }
        }

        cards.forEach { validCombinations.add(Combination(mutableListOf(it))) }

        return validCombinations.distinctBy { it.cards.toSet() }
            .sortedByDescending { it.value }
    }

    fun isGameOver(players: List<Player>, pointLimit: Int): Boolean {
        return players.any { it.score >= pointLimit }
    }

    fun colorsToRemove (nbOfPlayers: Int): Set<CardColor> {
        return if (nbOfPlayers <= Constants.GAME_REDUCED_COLOR_THRESHOLD) {
            Constants.DECK_REMOVED_COLORS
        } else {
            emptySet()
        }
    }


    fun hasPlayerWonTheRound(hand: Hand): Boolean {
        return hand.cards.isEmpty()
    }

    fun getGameWinners(players: List<Player>): List<Player> {
        val lowestScore = players.minOfOrNull { it.score } ?: return emptyList()
        return players.filter { it.score == lowestScore }
    }

    fun getPlayerRankings(players: List<Player>): List<Pair<Player, Int>> {
        return players.sortedBy { it.score }
            .mapIndexed { index, player -> player to (index + 1) }
    }

    fun updatePlayersScores(players: List<Player>) {
        TRACE(DEBUG) { "Updating scores" }
        for (player in players) {
            val scoreToAdd = player.hand.cards.size
            val newScore = player.score + scoreToAdd
            TRACE(DEBUG) { "Updating score for ${player.name} with ${scoreToAdd} cards, current score ${player.score} -> $newScore" }
            player.score = newScore
        }
    }

    private fun generateAllSubcombinations(cards: List<Card>): List<Combination> {
        val subsets = mutableListOf<Combination>()
        val size = cards.size

        for (subsetSize in 2..size) {
            val indices = (0 until size).toList()
            val combinations = indices.combinations(subsetSize)
            for (combinationIndices in combinations) {
                val subset = combinationIndices.map { cards[it] }
                subsets.add(Combination(subset.toMutableList()))
            }
        }

        return subsets
    }
}

fun <T> List<T>.combinations(k: Int): List<List<T>> {
    if (k > size) return emptyList()
    if (k == size) return listOf(this)
    if (k == 1) return map { listOf(it) }

    val result = mutableListOf<List<T>>()
    for (i in indices) {
        val elem = this[i]
        val remaining = subList(i + 1, size)
        val subCombinations = remaining.combinations(k - 1)
        for (subComb in subCombinations) {
            result.add(listOf(elem) + subComb)
        }
    }
    return result
}
