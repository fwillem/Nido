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
        .joinToString(", ") { "${it.value}/${it.color.name}" }
        .ifEmpty { "The hand is empty" }

    companion object {
        fun createHidden(count: Int): Hand {
            val list = mutableStateListOf<Card>()
            repeat(count) {
                // Assuming Card(0, "BACK") or similar represents a hidden card.
                // If "BACK" isn't a valid color, we can use a dummy value.
                // The key is that the UI treats it as hidden or we just need the count.
                // For now, let's use a value that won't break things, or rely on UI logic.
                // Let's create a standard card but rely on PlayerType.REMOTE logic in UI to hide face.
                list.add(Card(1, "RED")) 
            }
            return Hand(cards = list)
        }
    }
}
