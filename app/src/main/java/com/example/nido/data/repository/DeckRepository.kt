package com.example.nido.data.repository

import com.example.nido.data.model.Card
import com.example.nido.data.model.CardColor
import com.example.nido.data.model.CardResources
import com.example.nido.utils.Constants

object DeckRepository {

    fun generateDeck(shuffle: Boolean = false, removedColors: Set<CardColor> = emptySet()): MutableList<Card> {
        val deck = mutableListOf<Card>()
        for (color in CardColor.values()) {
            if (color !in removedColors) { // Add this check
                for (value in 1..Constants.HAND_SIZE) {
                    val cardImageId = CardResources.getImage(color, value) // Your original call
                    deck.add(Card(cardImageId, color, value = value)) // Your original order
                }
            }
        }
        if (shuffle) deck.shuffle()
        return deck
    }

    // Keep your other functions as they were
    fun getCardImage(color: CardColor, value: Int): Int {
        return CardResources.getImage(color, value)
    }

    fun getBackCover(): Card {
        return CardResources.backCoverCard
    }
}