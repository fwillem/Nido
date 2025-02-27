// GameState.kt
package com.example.nido.game

import com.example.nido.data.model.CardColor // Import CardColor
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Pile
import com.example.nido.data.model.Player

import com.example.nido.utils.Constants.REMOVED_COLORS


data class GameState(
    val screen: GameScreens = GameScreens.MENU,
    val numberOfPlayers: Int = 2,
    val maxPointLimit: Int = 15,
    val players: List<Player> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val currentCombination: Combination? = null,
    val discardPile: Pile = Pile(),
    val soundOn: Boolean = true,
    val showConfirmExitDialog: Boolean = false,
    val removedColors: Set<CardColor> = REMOVED_COLORS
)

enum class GameScreens {
    MENU,
    SETUP,
    PLAYING,
    GAMEOVER,
    ROUND_OVER
}
