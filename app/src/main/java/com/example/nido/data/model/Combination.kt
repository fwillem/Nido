
package com.example.nido.data.model


class Combination(val cards: MutableList<Card>) {  // ✅ No default value!

    fun addCard(card: Card) {
        this.cards.add(card)
    }

    // ✅ Compute the value, ensuring it's always valid
    val value: Int
        get() = if (cards.isEmpty()) 0 else cards
            .map { it.value }
            .sortedDescending()
            .joinToString("")
            .toInt()

    override fun toString(): String {
        return cards.joinToString { "${it.value} ${it.color}" }
    }
}

