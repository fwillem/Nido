package com.example.nido.utils

import androidx.compose.ui.graphics.Color
import com.example.nido.data.model.CardColor

// ✅ Constants file to centralize all game and UI constants.
object Constants {
    // 🎮 Game Rules
    const val GAME_MIN_POINT_LIMIT = 5
    const val GAME_DEFAULT_POINT_LIMIT = 15
    const val GAME_MAX_POINT_LIMIT = 30


    const val GAME_MIN_PLAYERS = 2
    const val GAME_MAX_PLAYERS = 6
    const val GAME_REDUCED_COLOR_THRESHOLD = 2 // If number of players is less or equal to this value, we use a reduced set of colors

    val DECK_REMOVED_COLORS: Set<CardColor> = setOf(
        CardColor.RED,
        CardColor.ORANGE
    )

    const val CARD_MAX_VALUE = 9

    const val HAND_SIZE = 9

    // 📏 🎨 UI Dimensions & Colors
    const val CARD_ON_HAND_HEIGHT = 160
    const val CARD_ON_HAND_WIDTH = (CARD_ON_HAND_HEIGHT/2)// 80

    const val CARD_ON_MAT_HEIGHT = 160
    const val CARD_ON_MAT_WIDTH = (CARD_ON_MAT_HEIGHT/2)

    const val PLAYERS_ROW_HEIGHT = 50
    const val MAT_VIEW_HEIGHT = 150

    const val SELECTED_CARD_OFFSET = 32
    /*
    const val PLAYERS_ROW_COLOR = 0xFFF06400 // OK, This one is OK

    const val MAT_VIEW_COLOR = 0xFF228B22 // OK, This one is OK

    const val MAIN_SCREEN_COLOR = 0xFF006400


    const val HAND_VIEW_COLOR2 = 0xFF0000FF


     */

    // UI General
    const val NB_OF_DISCARDED_CARDS_TO_SHOW = 4
    const val AI_THINKING_DURATION_MS = 1200L

    const val DEFAULT_LOCAL_PLAYER_NAME = "Jil"
    const val DEFAULT_LOCAL_PLAYER_AVATAR = "👩"

    const val DEFAULT_SECOND_PLAYER_NAME = "Bibi"
    const val DEFAULT_SECOND_PLAYER_AVATAR = "👤"

    const val SPLASH_SCREEN_TIMEOUT = 4000L

    // APP CONSTANTS
    const val DATASTORE_NAME = "nido_preferences"

}
