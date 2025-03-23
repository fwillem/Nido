package com.example.nido.utils

import com.example.nido.data.model.Card
import kotlin.text.get

enum class SortMode { FIFO, COLOR, VALUE }
//enum class SortMode { COLOR, VALUE }



fun List<Card>.sortedByComplexCriteria(): List<Card> {
    val groupedByColor = this.groupBy { it.color }
    val colorOrder = groupedByColor.entries.sortedByDescending { entry ->
        entry.value.joinToString("") { it.value.toString() }.toIntOrNull() ?: 0
    }.map { it.key }
    val sortedCards = mutableListOf<Card>()
    for (color in colorOrder) {
        val cardsOfColor = groupedByColor[color] ?: emptyList()
        sortedCards.addAll(cardsOfColor.sortedByDescending { it.value })
    }
    return sortedCards
}

fun List<Card>.sortedByMode(mode: SortMode): List<Card> {
    return when (mode) {
        SortMode.FIFO -> this // No sorting; keep the original order.
        // In COLOR mode, sort by color (using the ordinal) and then by value in descending order.
        SortMode.COLOR -> this.sortedWith(compareBy<Card> { it.color.ordinal }
            .thenByDescending { it.value })
        // In VALUE mode, sort all cards by descending value.
        SortMode.VALUE -> this.sortedByDescending { it.value }
    }
}
