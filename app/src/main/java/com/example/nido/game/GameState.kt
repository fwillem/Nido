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
    val deck: SnapshotStateList<Card> = mutableStateListOf(), // Added deck
    val soundOn: Boolean = true,
    val showConfirmExitDialog: Boolean = false
) {
    override fun toString(): String {
        // 👥 Players: ${players.joinToString("\n") { "   - $it" }}

        return """
            🔍 GameState Debug Info:
            💠 Screen: $screen
            💠 Number of Players: $numberOfPlayers.
            💠 Point Limit: $pointLimit
            💠 Nb of players: ${players.size }}
            💠 Current Player Index: $currentPlayerIndex
            💠 Current Combination on Mat: ${currentCombinationOnMat ?: "None"}
            💠 Discard Pile: ${discardPile.joinToString(", ") { it.toString() }}
            💠 Deck: ${deck.joinToString(", ") { it.toString() }}
            💠 Sound On: $soundOn
            💠 Confirm Exit Dialog: $showConfirmExitDialog
        """.trimIndent()
    }
    fun deepCopy(): GameState {
        return GameState(
            screen = this.screen,
            numberOfPlayers = this.numberOfPlayers,
            pointLimit = this.pointLimit,
            players = this.players.map { it.copy() }, // Deep copy players
            currentPlayerIndex = this.currentPlayerIndex,
            currentCombinationOnMat = this.currentCombinationOnMat?.let { Combination(it.cards.toMutableList()) },
            discardPile = mutableStateListOf<Card>().apply { addAll(this@GameState.discardPile) },
            deck = mutableStateListOf<Card>().apply { addAll(this@GameState.deck) },
            soundOn = this.soundOn,
            showConfirmExitDialog = this.showConfirmExitDialog
        )
    }

}

enum class GameScreens {
    MENU,
    SETUP,
    PLAYING,
    GAMEOVER,
    ROUND_OVER
}
