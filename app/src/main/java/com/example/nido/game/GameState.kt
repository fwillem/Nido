package com.example.nido.game

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.utils.Constants

data class GameState(
    val screen: GameScreens = GameScreens.MENU,
    val numberOfPlayers: Int = 2,
    val pointLimit: Int = Constants.GAME_DEFAULT_POINT_LIMIT,
    val players: List<Player> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val currentCombinationOnMat: Combination? = null, // Renamed variable
    val discardPile: SnapshotStateList<Card> = mutableStateListOf(),
    val soundOn: Boolean = true,
    val showConfirmExitDialog: Boolean = false
)

enum class GameScreens {
    MENU,
    SETUP,
    PLAYING,
    GAMEOVER,
    ROUND_OVER
}