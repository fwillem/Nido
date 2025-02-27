package com.example.nido.utils

import com.example.nido.data.model.CardColor

// ✅ Constants file to centralize all game and UI constants.
object Constants {
    // 🎮 Game Rules
    const val GAME_MIN_POINT_LIMIT = 10
    const val GAME_DEFAULT_POINT_LIMIT = 15
    const val GAME_MAX_POINT_LIMIT = 30


    const val GAME_MIN_PLAYERS = 2
    const val GAME_MAX_PLAYERS = 6
    const val GAME_REDUCED_COLOR_THRESHOLD = 2 // If number of players is less or equal to this value, we use a reduced set of colors

    val REMOVED_COLORS: Set<CardColor> = setOf( // Corrected: Use setOf
        CardColor.RED,  // Corrected: Use enum constants
        CardColor.ORANGE
    )

    const val HAND_SIZE = 9

    // 📏 UI Dimensions
    const val CARD_WIDTH = 50
    const val CARD_HEIGHT = 100

}
