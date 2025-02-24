package com.example.nido.data.model


class Combination(val cards: MutableList<Card> = mutableListOf()) {
    fun addCard(card: Card) {
        this.cards.add(card)
    }

    // Compute the value by sorting digits in descending order and forming a number
    val value: Int
        get() = cards
            .map { it.value }
            .sortedDescending()
            .joinToString("")
            .toInt()
}
