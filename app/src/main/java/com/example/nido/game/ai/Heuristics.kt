// AIHeuristicsEngine.kt
package com.example.nido.game.ai

import com.example.nido.data.model.*
import com.example.nido.game.rules.GameRules
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.DEBUG

/**
 * Heuristic engine for determining AI move logic.
 */
object AIHeuristicsEngine {

    data class Decision(
        val combinationToPlay: Combination?,
        val cardToKeep: Card?,
        val shouldSkip: Boolean
    )

    fun evaluateMove(hand: List<Card>, playmat: Combination, level: AILevel): Decision {
        val allValid = GameRules.findValidCombinations(hand)

        val playable = allValid.filter {
            GameRules.isValidMove(playmat, it, hand)
        }.sortedByDescending { it.value }

        if (playable.isEmpty()) {
            TRACE(DEBUG) { "[AI] No valid move found → skipping" }
            return Decision(null, null, shouldSkip = true)
        }

        val chosenCombo = when (level) {
            AILevel.BEGINNER -> {
                val combo = playable.first()
                TRACE(DEBUG) { "[AI-BEGINNER] Choosing first valid combination: ${combo.cards.joinToString()}" }
                combo
            }
            AILevel.ADVANCED -> {
                val best = playable.firstOrNull { !createsSingleton(it, hand) }
                if (best != null) {
                    TRACE(DEBUG) {
                        "[AI-ADVANCED] Selected combination avoiding singleton: ${best.cards.joinToString()}"
                    }
                    best
                } else {
                    val fallback = playable.first()
                    TRACE(DEBUG) {
                        "[AI-ADVANCED] No combination avoids singleton. Fallback to: ${fallback.cards.joinToString()}"
                    }
                    fallback
                }
            }
        }

        // 🔹 Card to keep: logic now depends on AI level
        val cardToKeep = when (level) {
            AILevel.BEGINNER -> playmat.cards.firstOrNull().also {
                TRACE(DEBUG) { "[AI-BEGINNER] Keeps first card on mat: $it" }
            }
            AILevel.ADVANCED -> selectCardToKeep(playmat.cards, hand)
        }

        return Decision(chosenCombo, cardToKeep, shouldSkip = false)
    }

    private fun createsSingleton(candidate: Combination, fullHand: List<Card>): Boolean {
        val remaining = fullHand - candidate.cards.toSet()
        return remaining.any { isSingleton(it, remaining) }
    }

    private fun isSingleton(card: Card, others: List<Card>): Boolean {
        val colorMatches = others.count { it.color == card.color }
        val valueMatches = others.count { it.value == card.value }
        return colorMatches == 0 && valueMatches == 0
    }

    private fun selectCardToKeep(previousMat: List<Card>, hand: List<Card>): Card? {
        val notSingleton = previousMat.firstOrNull { !isSingleton(it, hand) }
        if (notSingleton != null) {
            TRACE(DEBUG) { "[AI-ADVANCED] Keeps card that is not a singleton: $notSingleton" }
            return notSingleton
        }

        val helpsSingleton = previousMat.firstOrNull { helpsSingleton(it, hand) }
        if (helpsSingleton != null) {
            TRACE(DEBUG) { "[AI-ADVANCED] Keeps card that helps reduce singleton: $helpsSingleton" }
            return helpsSingleton
        }

        val bestPotential = previousMat.maxByOrNull { valueForPotential(it, hand) }
        TRACE(DEBUG) { "[AI-ADVANCED] Keeps card with best combination potential: $bestPotential" }
        return bestPotential
    }

    private fun helpsSingleton(candidate: Card, hand: List<Card>): Boolean {
        return hand.any {
            isSingleton(it, hand) && (it.color == candidate.color || it.value == candidate.value)
        }
    }

    private fun valueForPotential(card: Card, hand: List<Card>): Int {
        val sameColor = hand.count { it.color == card.color }
        val sameValue = hand.count { it.value == card.value }
        return sameColor + sameValue
    }
}
