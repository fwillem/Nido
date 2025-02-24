package com.example.nido.data.repository

import com.example.nido.R


package com.example.nido.data.repository

import com.example.nido.data.Card
import com.example.nido.data.CardColor
import com.example.nido.data.CardResources

class CardRepository {

    // Generate a full deck of cards
    fun generateDeck(shuffle: Boolean = false): MutableList<Card> {
        val deck = mutableListOf<Card>()
        for (color in CardColor.values()) {
            for (value in 1..9) {
                val cardImageId = CardResources.getImage(color, value)
                deck.add(Card(cardImageId, color, value = value))
            }
        }
        if (shuffle) deck.shuffle()
        return deck
    }

    // Get a specific card image
    fun getCardImage(color: CardColor, value: Int): Int {
        return CardResources.getImage(color, value)
    }

    // Get the back cover card
    fun getBackCover(): Card {
        return CardResources.backCoverCard
    }
}
