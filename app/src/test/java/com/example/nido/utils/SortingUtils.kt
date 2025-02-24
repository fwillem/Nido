package com.example.nido.utils

import com.example.nido.data.model.Card

enum class SortMode { FIFO, COLOR, VALUE }

fun List<Card>.sortedByMode(mode: SortMode): List<Card> {
    return when (mode) {
        SortMode.FIFO -> this
        SortMode.COLOR -> this
        SortMode.VALUE -> this.sortedByDescending { it.value }
    }
}

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
