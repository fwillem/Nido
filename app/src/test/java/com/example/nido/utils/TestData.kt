package com.example.nido.utils

import androidx.compose.material3.Card
import com.example.nido.data.model.*

val testVectors = listOf(
    ::generateTestHand1,
    ::generateTestHand2,
    ::generateTestHand3,
    ::generateTestHand4,
    ::generateTestHand5,
    ::generateTestHand6
)

fun generateTestHand1(): Hand {
    return Hand().apply {
        addCard(Card(CardResources.getImage(CardColor.BLUE, 1), CardColor.BLUE, value = 1))
        addCard(Card(CardResources.getImage(CardColor.RED, 7), CardColor.RED, value = 7))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 9), CardColor.MOCHA, value = 9))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 2), CardColor.MOCHA, value = 2))
        addCard(Card(CardResources.getImage(CardColor.RED, 3), CardColor.RED, value = 3))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 5), CardColor.GREEN, value = 5))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 8), CardColor.GREEN, value = 8))
        addCard(Card(CardResources.getImage(CardColor.RED, 9), CardColor.RED, value = 9))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 1), CardColor.MOCHA, value = 1))
    }
}

fun generateTestHand2(): Hand {
    return Hand().apply {
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 1), CardColor.MOCHA, value = 1))
        addCard(Card(CardResources.getImage(CardColor.RED, 7), CardColor.RED, value = 7))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 9), CardColor.MOCHA, value = 9))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 2), CardColor.MOCHA, value = 2))
        addCard(Card(CardResources.getImage(CardColor.RED, 3), CardColor.RED, value = 3))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 5), CardColor.GREEN, value = 5))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 8), CardColor.GREEN, value = 8))
        addCard(Card(CardResources.getImage(CardColor.RED, 9), CardColor.RED, value = 9))
        addCard(Card(CardResources.getImage(CardColor.RED, 1), CardColor.RED, value = 1))
    }
}

fun generateTestHand3(): Hand {
    return Hand().apply {
        addCard(Card(CardResources.getImage(CardColor.RED, 9), CardColor.RED, value = 9))
        addCard(Card(CardResources.getImage(CardColor.RED, 8), CardColor.RED, value = 8))
        addCard(Card(CardResources.getImage(CardColor.RED, 7), CardColor.RED, value = 7))
        addCard(Card(CardResources.getImage(CardColor.RED, 6), CardColor.RED, value = 6))
        addCard(Card(CardResources.getImage(CardColor.RED, 5), CardColor.RED, value = 5))
        addCard(Card(CardResources.getImage(CardColor.RED, 4), CardColor.RED, value = 4))
        addCard(Card(CardResources.getImage(CardColor.RED, 3), CardColor.RED, value = 3))
        addCard(Card(CardResources.getImage(CardColor.BLUE, 2), CardColor.BLUE, value = 2))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 2), CardColor.MOCHA, value = 2))
    }
}

fun generateTestHand4(): Hand {
    return Hand().apply {
        addCard(Card(CardResources.getImage(CardColor.RED, 9), CardColor.RED, value = 9))
        addCard(Card(CardResources.getImage(CardColor.BLUE, 8), CardColor.BLUE, value = 8))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 7), CardColor.GREEN, value = 7))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 6), CardColor.MOCHA, value = 6))
        addCard(Card(CardResources.getImage(CardColor.PINK, 5), CardColor.PINK, value = 5))
        addCard(Card(CardResources.getImage(CardColor.ORANGE, 4), CardColor.ORANGE, value = 4))
        addCard(Card(CardResources.getImage(CardColor.BLUE, 3), CardColor.BLUE, value = 3))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 2), CardColor.GREEN, value = 2))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 1), CardColor.MOCHA, value = 1))
    }
}

fun generateTestHand5(): Hand {
    return Hand().apply {
        addCard(Card(CardResources.getImage(CardColor.RED, 9), CardColor.RED, value = 9))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 9), CardColor.MOCHA, value = 9))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 8), CardColor.GREEN, value = 8))
        addCard(Card(CardResources.getImage(CardColor.ORANGE, 7), CardColor.ORANGE, value = 7))
        addCard(Card(CardResources.getImage(CardColor.PINK, 7), CardColor.PINK, value = 7))
        addCard(Card(CardResources.getImage(CardColor.BLUE, 5), CardColor.BLUE, value = 5))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 4), CardColor.MOCHA, value = 4))
        addCard(Card(CardResources.getImage(CardColor.RED, 3), CardColor.RED, value = 3))
        addCard(Card(CardResources.getImage(CardColor.ORANGE, 2), CardColor.ORANGE, value = 2))
    }
}

fun generateTestHand6(): Hand {
    return Hand().apply {
        addCard(Card(CardResources.getImage(CardColor.RED, 7), CardColor.RED, value = 7))
        addCard(Card(CardResources.getImage(CardColor.BLUE, 7), CardColor.BLUE, value = 7))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 7), CardColor.GREEN, value = 7))
        addCard(Card(CardResources.getImage(CardColor.MOCHA, 7), CardColor.MOCHA, value = 7))
        addCard(Card(CardResources.getImage(CardColor.PINK, 3), CardColor.PINK, value = 3))
        addCard(Card(CardResources.getImage(CardColor.ORANGE, 3), CardColor.ORANGE, value = 3))
        addCard(Card(CardResources.getImage(CardColor.RED, 3), CardColor.RED, value = 3))
        addCard(Card(CardResources.getImage(CardColor.BLUE, 9), CardColor.BLUE, value = 9))
        addCard(Card(CardResources.getImage(CardColor.GREEN, 1), CardColor.GREEN, value = 1))
    }
}

fun Hand.addSampleCards() {
    addCard(Card(CardResources.getImage(CardColor.RED, 9), CardColor.RED, value = 9))
    addCard(Card(CardResources.getImage(CardColor.BLUE, 8), CardColor.BLUE, value = 8))
    addCard(Card(CardResources.getImage(CardColor.GREEN, 7), CardColor.GREEN, value = 7))
}

