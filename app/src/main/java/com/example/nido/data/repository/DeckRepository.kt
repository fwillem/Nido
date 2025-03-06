package com.example.nido.data.repository

import androidx.compose.runtime.mutableStateListOf
import com.example.nido.data.model.Card
import com.example.nido.data.model.CardColor
import com.example.nido.data.model.CardResources
import com.example.nido.game.rules.GameRules
import com.example.nido.utils.Constants


val testDeckFor2 = mutableStateListOf<Card>(
    Card(value = 5, color = "MOCHA"),
    Card(value = 2, color = "BLUE"),
    Card(value = 9, color = "MOCHA"),
    Card(value = 6, color = "PINK"),
    Card(value = 3, color = "MOCHA"),
    Card(value = 1, color = "GREEN"),
    Card(value = 7, color = "MOCHA"),
    Card(value = 6, color = "GREEN"),
    Card(value = 1, color = "PINK"),
    Card(value = 8, color = "BLUE"),
    Card(value = 4, color = "GREEN"),
    Card(value = 2, color = "PINK"),
    Card(value = 6, color = "BLUE"),
    Card(value = 5, color = "PINK"),
    Card(value = 8, color = "GREEN"),
    Card(value = 5, color = "GREEN"),
    Card(value = 9, color = "PINK"),
    Card(value = 1, color = "BLUE"),
    Card(value = 4, color = "BLUE"),
    Card(value = 1, color = "MOCHA"),
    Card(value = 8, color = "PINK"),
    Card(value = 3, color = "BLUE"),
    Card(value = 2, color = "MOCHA"),
    Card(value = 8, color = "MOCHA"),
    Card(value = 5, color = "BLUE"),
    Card(value = 3, color = "PINK"),
    Card(value = 7, color = "BLUE"),
    Card(value = 7, color = "GREEN"),
    Card(value = 2, color = "GREEN"),
    Card(value = 9, color = "GREEN"),
    Card(value = 4, color = "PINK"),
    Card(value = 4, color = "MOCHA"),
    Card(value = 7, color = "PINK"),
    Card(value = 6, color = "MOCHA"),
    Card(value = 9, color = "BLUE"),
    Card(value = 3, color = "GREEN")
)

val testDeckForAll = mutableStateListOf<Card>(
    Card(value = 4, color = "PINK"),
    Card(value = 8, color = "RED"),
    Card(value = 7, color = "RED"),
    Card(value = 8, color = "PINK"),
    Card(value = 7, color = "BLUE"),
    Card(value = 3, color = "BLUE"),
    Card(value = 6, color = "GREEN"),
    Card(value = 7, color = "MOCHA"),
    Card(value = 2, color = "MOCHA"),
    Card(value = 9, color = "MOCHA"),
    Card(value = 3, color = "GREEN"),
    Card(value = 2, color = "RED"),
    Card(value = 5, color = "RED"),
    Card(value = 1, color = "GREEN"),
    Card(value = 3, color = "PINK"),
    Card(value = 6, color = "RED"),
    Card(value = 2, color = "PINK"),
    Card(value = 5, color = "ORANGE"),
    Card(value = 4, color = "ORANGE"),
    Card(value = 7, color = "PINK"),
    Card(value = 1, color = "RED"),
    Card(value = 5, color = "PINK"),
    Card(value = 3, color = "MOCHA"),
    Card(value = 3, color = "RED"),
    Card(value = 9, color = "GREEN"),
    Card(value = 9, color = "PINK"),
    Card(value = 2, color = "ORANGE"),
    Card(value = 5, color = "GREEN"),
    Card(value = 1, color = "PINK"),
    Card(value = 6, color = "BLUE"),
    Card(value = 8, color = "GREEN"),
    Card(value = 4, color = "MOCHA"),
    Card(value = 9, color = "RED"),
    Card(value = 6, color = "MOCHA"),
    Card(value = 5, color = "MOCHA"),
    Card(value = 1, color = "ORANGE"),
    Card(value = 5, color = "BLUE"),
    Card(value = 4, color = "BLUE"),
    Card(value = 8, color = "BLUE"),
    Card(value = 1, color = "MOCHA"),
    Card(value = 6, color = "ORANGE"),
    Card(value = 4, color = "GREEN"),
    Card(value = 8, color = "ORANGE"),
    Card(value = 2, color = "GREEN"),
    Card(value = 9, color = "BLUE"),
    Card(value = 8, color = "MOCHA"),
    Card(value = 3, color = "ORANGE"),
    Card(value = 4, color = "RED"),
    Card(value = 2, color = "BLUE"),
    Card(value = 7, color = "ORANGE"),
    Card(value = 9, color = "ORANGE"),
    Card(value = 7, color = "GREEN"),
    Card(value = 1, color = "BLUE"),
    Card(value = 6, color = "PINK")
)





    object DeckRepository {

    fun generateDeck(shuffle: Boolean = false, nbOfPlayers: Int) : List<Card> {
        val deck = mutableListOf<Card>()


        val removedColors = GameRules.colorsToRemove(nbOfPlayers)

        for (color in CardColor.values()) {
            if (color !in removedColors) { // Add this check
                for (value in 1..Constants.CARD_MAX_VALUE) {
                    val cardImageId = CardResources.getImage(color, value) // Your original call
                    deck.add(Card(cardImageId, color, value = value)) // Your original order
                }
            }
        }

        if (shuffle) deck.shuffle()


        if (removedColors.isNotEmpty()) {
            return testDeckFor2
        } else {
            return testDeckForAll
        }



        return deck
    }

    fun shuffleDeck(deck: List<Card>) : List<Card> {
        // TODO Implement shuffle return(deck.shuffled())
        return(deck)
        // return deck.shuffled()
    }

    // Keep your other functions as they were
    fun getCardImage(color: CardColor, value: Int): Int {
        return CardResources.getImage(color, value)
    }

    fun getBackCover(): Card {
        return CardResources.backCoverCard
    }
}