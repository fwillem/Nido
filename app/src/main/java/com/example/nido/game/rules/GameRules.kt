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

    fun isValidMove(current: Combination, newMove: Combination, handSize: Int): Boolean {
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

            // The move is valid if the new combination is better than the current one and :
            // the number of cards in the new combination is not greater than the number of cards in the current combination + 1
            // or he plays all the cards of his hand
            // TODO TOREMOVE SHALL USE A gameManager function ot check if use won the round instead of checking handSize
            val isValid = (newMove.value > current.value) && ((newMove.cards.size <= current.cards.size + 1) || (handSize == 0))

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
        return players.any { it.score <= 0 }
    }

    fun colorsToRemove (nbOfPlayers: Int): Set<CardColor> {
        return if (nbOfPlayers <= Constants.GAME_REDUCED_COLOR_THRESHOLD) {
            Constants.DECK_REMOVED_COLORS
        } else {
            emptySet()
        }
    }


    fun hasPlayerWonTheRound(hand: Hand): Boolean {
        TRACE(VERBOSE) { "Checking if player has won the round: ${hand.cards} (${hand.cards.isEmpty()} - ${hand.cards.size})" }
        return hand.cards.isEmpty()
    }

    // Now if there are players with the same score, all of them win!
    fun getGameWinners(players: List<Player>): List<Player> {
        val highestScore = players.maxOfOrNull { it.score } ?: return emptyList()
        return players.filter { it.score == highestScore }
    }

    fun getPlayerRankings(players: List<Player>): List<Pair<Player, Int>> {
        return players
            .sortedByDescending { it.score }
            .mapIndexed { index, player ->
                player to (index + 1) }
    }

    fun getPlayerHandScores(players: List<Player>) : List<Pair<Player, Int>> {
        // We need to return here for each player the number of cards he has (since it will be the score that will be added to the player's score)
        return players.map { player ->
            player to player.hand.cards.size
        }
    }


    fun updatePlayersScores(players: List<Player>) {
        TRACE(DEBUG) { "Updating scores" }
        for (player in players) {
            val scoreToRetreive = player.hand.cards.size
            val newScore = player.score - scoreToRetreive
            TRACE(DEBUG) { "Updating score for ${player.name} with -${scoreToRetreive} cards, current score ${player.score} -> $newScore" }
            player.score = newScore
        }
    }

    fun initializePlayerScores(players: List<Player>, pointLimit: Int): List<Player> {
        return players.map { player ->
            player.copy(score = pointLimit)
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
