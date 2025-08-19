package com.example.nido.game.rules

import com.example.nido.data.model.Card
import com.example.nido.data.model.CardColor
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*
import com.example.nido.utils.Constants
import com.example.nido.data.model.Hand
import com.example.nido.data.model.*
import com.example.nido.game.*


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


    fun isValidMove(current: Combination, newMove: Combination, hand: List<Card>): Boolean {
        return try {
            if (newMove.cards.isEmpty()) {
                TRACE(VERBOSE) { "New move has no cards!" }
                return false
            }

            if (!isValidCombination(newMove)) {
                TRACE(VERBOSE) { "New move is not valid!" }
                return false
            }

            val isPlayingAllCards = hand.all { it.isSelected }
            val isValid = (newMove.value > current.value) &&
                    ((newMove.cards.size <= current.cards.size + 1) || isPlayingAllCards)

            TRACE(VERBOSE) {
                "isValidMove: Current = ${current.value}, New = ${newMove.value}, " +
                        "Card Size = ${newMove.cards.size}, Current Size = ${current.cards.size}, Allowed = $isValid"
            }
            isValid

        } catch (e: Exception) {
            TRACE(FATAL) { "❗❗❗❗ FATAL ERROR in isValidMove: ${e.message}" }
            false
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



fun calculateTurnInfo(gameState: GameState): TurnInfo {
    val currentPlayer = gameState.players[gameState.currentPlayerIndex]
    val isLocal = currentPlayer.playerType == PlayerType.LOCAL
    val isAI = currentPlayer.playerType == PlayerType.AI
    val isRemote = currentPlayer.playerType == PlayerType.REMOTE


    val handCards = currentPlayer.hand.cards
    val selectedCards = handCards.filter { it.isSelected }
    val unselectedCards = handCards.filter { !it.isSelected }

    val hasSelection = selectedCards.isNotEmpty()
    val selectedCombination = Combination(selectedCards.toMutableList())
    val playmatCombo = gameState.currentCombinationOnMat

    // ────────────────────────────────────────────────
    // ⚠️ 1. Special Case: First move of the round
    //    → When playmat is empty and no one has skipped yet
    // ────────────────────────────────────────────────
    val isFirstMoveOfRound =
        playmatCombo.cards.isEmpty() && gameState.skipCount == 0

    // 🟢 Can the user go all-in? (i.e., play their whole hand at once as a valid combo)
    // CAUTION THeir was a bug, we corrected it
    /*
    val canGoAllIn =
        isFirstMoveOfRound &&
                unselectedCards.isEmpty() && // All cards selected
                GameRules.isValidCombination(Combination(handCards.toMutableList()))

     */
    val canGoAllIn =
        isFirstMoveOfRound &&
                GameRules.isValidCombination(Combination(handCards.toMutableList()))

    // ────────────────────────────────────────────────
    // 🃏 2. Validate current selection
    //    → Is the current selection a valid move?
    // ────────────────────────────────────────────────
    val isSelectionValid = hasSelection &&
            GameRules.isValidMove(
                current = playmatCombo,
                newMove = selectedCombination,
                hand = handCards
            )

    // ────────────────────────────────────────────────
    // 🔍 3. Can the player play *any* valid move?
    //    → Explore all valid combinations from hand
    // ────────────────────────────────────────────────
    val possibleMoves = GameRules.findValidCombinations(handCards)
    val canPlayAny = possibleMoves.any {
        GameRules.isValidMove(
            playmatCombo,
            it,
            handCards
        )
    }

    // ────────────────────────────────────────────────
    // ⛔ 4. Should the player skip?
    // ────────────────────────────────────────────────
    val mustSkip = !canPlayAny && handCards.isNotEmpty()
    val canSkip = !isFirstMoveOfRound // Skipping not allowed on very first move

    // ────────────────────────────────────────────────
    // 🎯 5. Determine which action button should show
    //     (Play, Skip, SkipWithCounter)
    // ────────────────────────────────────────────────
    val (displayPlay, displaySkip, displaySkipCounter) = when {
        isSelectionValid -> Triple(true, false, false)
        mustSkip         -> Triple(false, false, true)
        canSkip          -> Triple(false, true, false)
        else             -> Triple(false, false, false)
    }

    // 🗑️ 6. Show "Remove" button if there is any selection
    val displayRemove = hasSelection

    // 🧨 Invariant Check: Only one main action button can be shown at once
    val activeFlags = listOf(displayPlay, displaySkip, displaySkipCounter)
    if (activeFlags.count { it } > 1) {
        TRACE(FATAL) {
            "💥 Inconsistent TurnInfo: multiple main buttons are active simultaneously!\n" +
                    "→ displayPlay=$displayPlay, displaySkip=$displaySkip, displaySkipCounter=$displaySkipCounter\n" +
                    "→ GameState: $gameState"
        }
    }

    // SHow Manual AI Play button if manual mode selected
    val displayManualAIPlay = isAI && gameState.doNotAutoPlayAI

    // Placeholder to remote player notification
    val displayNotifyRemotePlayer = isRemote //

    TRACE(VERBOSE) {
        "!!!! displayManualAIPlay = $displayManualAIPlay,(currentPlayer.playerType = ${currentPlayer.playerType}), " +
                "gameState.doNotAutoPlayAI = ${gameState.doNotAutoPlayAI}"
    }


    // ✅ Return the final TurnInfo
    return TurnInfo(
        canSkip = canSkip,
        canGoAllIn = canGoAllIn,
        displayPlay = displayPlay && isLocal,
        displaySkip = displaySkip && isLocal,
        displaySkipCounter = displaySkipCounter && isLocal,
        displayRemove = displayRemove && isLocal,
        displayManualAIPlay = displayManualAIPlay,
        displayNotifyRemotePlayer = displayNotifyRemotePlayer
    )
}
