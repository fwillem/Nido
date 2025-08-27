package com.example.nido.utils

import androidx.compose.ui.graphics.Color
import com.example.nido.data.model.CardColor

// ✅ Constants file to centralize all game and UI constants.
object Constants {
    // 🎮 Game Rules
    const val GAME_MIN_POINT_LIMIT = 5
    const val GAME_DEFAULT_POINT_LIMIT = 15
    const val GAME_MAX_POINT_LIMIT = 30


    const val GAME_MIN_PLAYERS = 2 // Caution, Cannot be less than 1
    const val GAME_MAX_PLAYERS = 6
    const val GAME_REDUCED_COLOR_THRESHOLD =
        2 // If number of players is less or equal to this value, we use a reduced set of colors

    val DECK_REMOVED_COLORS: Set<CardColor> = setOf(
        CardColor.RED,
        CardColor.ORANGE
    )

    const val CARD_MAX_VALUE = 9

    const val HAND_SIZE = 9

    // 📏 🎨 UI Dimensions & Colors
    const val CARD_ON_HAND_HEIGHT = 160
    const val CARD_ON_HAND_WIDTH = (CARD_ON_HAND_HEIGHT / 2)// 80

    const val CARD_ON_MAT_HEIGHT = 120
    const val CARD_ON_MAT_WIDTH = (CARD_ON_MAT_HEIGHT / 2)

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


    // in milliseconds
    const val AI_THINKING_DURATION_MIN = 500
    const val AI_THINKING_DURATION_DEFAULT = 1200
    const val AI_THINKING_DURATION_MAX = 3000
    const val AI_THINKING_DURATION_STEPS = 300


    const val DEFAULT_LOCAL_PLAYER_NAME = "Jil"
    const val DEFAULT_LOCAL_PLAYER_AVATAR = "👩"

    const val DEFAULT_SECOND_PLAYER_NAME = "Bibi"
    const val DEFAULT_SECOND_PLAYER_AVATAR = "👤"

    const val SPLASH_SCREEN_TIMEOUT = 4000L

    // APP CONSTANTS
    const val DATASTORE_NAME = "nido_preferences"

    // UID

    // 🔧 Dev override (ON/OFF) + GameID commun aux 2 appareils
    const val DEV_FORCE_ROUTING = true
    const val DEV_FORCE_GAME_ID = "71f98cc7-7ac5-42b9-93ed-c60a456f4e2c" // <<< même valeur sur Xiaomi & VD

    /* XIAOMI CONFIG */
    /*
    const val ME_UID    = "L5vLf2aWLucgAs29hL5F7iPR7B92"
    const val OTHER_UID = "4175Z45HfMOP5tOCDqLFlFQ0FJt2"

*/

    /* VD CONFIG


    const val OTHER_UID    = "L5vLf2aWLucgAs29hL5F7iPR7B92"
    const val ME_UID  = "4175Z45HfMOP5tOCDqLFlFQ0FJt2"
*/

    object RemoteTestIds {
        const val ANONYMOUS_UID_REMOTE_PLAYER_XIAOMI    = "L5vLf2aWLucgAs29hL5F7iPR7B92"
        const val ANONYMOUS_UID_REMOTE_PLAYER_VD        = "4175Z45HfMOP5tOCDqLFlFQ0FJt2"
    }


    /*
    object RemoteTestIds {
        const val ANONYMOUS_UID_REMOTE_PLAYER_XIAOMI    = "L5vLf2aWLucgAs29hL5F7i-GUEST"
        const val ANONYMOUS_UID_REMOTE_PLAYER_VD        = "4175Z45HfMOP5tOCDqLFlFQ-HOST"
    }

     */


}
