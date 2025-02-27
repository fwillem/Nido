package com.example.nido.data.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.nido.game.rules.GameRules

data class Hand(
    val cards: SnapshotStateList<Card> = mutableStateListOf(),
    val combinations: SnapshotStateList<Combination> = mutableStateListOf()
) {
    fun addCard(card: Card) {
        cards.add(card)
        updateCombinations()
    }

    fun removeCard(card: Card): Boolean {
        val removed = cards.remove(card)
        if (removed) updateCombinations()
        return removed
    }

    fun removeCard(index: Int = 0): Card? = cards.getOrNull(index)?.also {
        cards.removeAt(index)
        updateCombinations()
    }

    fun removeCombination(combination: Combination): Boolean {
        if (!combination.cards.all { it in cards }) return false
        combination.cards.forEach { cards.remove(it) }
        updateCombinations()
        return true
    }

    fun clear() {
        cards.clear()
        combinations.clear()
    }

    fun isEmpty(): Boolean = cards.isEmpty()
    fun count(): Int = cards.size

    fun updateCombinations() {
        combinations.clear()
        val newCombinations = GameRules.findValidCombinations(cards) // ✅ Ensures it calls GameRules correctly
        combinations.addAll(newCombinations)
    }

    // Crucial: Deep copy of the Hand
    fun copy(): Hand {
        val newHand = Hand()
        newHand.cards.addAll(this.cards)
        return newHand
    }

    override fun toString(): String = cards
        .joinToString(", ") { "${it.color.name} ${it.value}" }
        .ifEmpty { "The hand is empty" }
}
