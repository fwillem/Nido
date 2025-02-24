package com.example.nido.data.repository

import com.example.nido.data.model.Card
import com.example.nido.data.model.CardColor
import com.example.nido.data.model.CardResources
import com.example.nido.game.rules.HAND_SIZE

object DeckRepository {

    fun generateDeck(shuffle: Boolean = false): MutableList<Card> {
        val deck = mutableListOf<Card>()
        for (color in CardColor.values()) {
            for (value in 1..HAND_SIZE) {
                val cardImageId = CardResources.getImage(color, value)
                deck.add(Card(cardImageId, color, value = value))
            }
        }
        if (shuffle) deck.shuffle()
        return deck
    }
}
