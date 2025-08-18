// GameState.kt  (FULL — replace file)
package com.example.nido.game

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.events.AppDialogEvent
import com.example.nido.events.GameDialogEvent
import com.example.nido.utils.Constants

data class GameState(
    val playerId: String = "",
    val turnInfo: TurnInfo = TurnInfo(),
    val doNotAutoPlayAI: Boolean = false,
    val pointLimit: Int = Constants.GAME_DEFAULT_POINT_LIMIT,

    val players: List<Player> = emptyList(),
    val startingPlayerIndex: Int = 0,
    val currentPlayerIndex: Int = 0,

    val currentCombinationOnMat: Combination = Combination(mutableListOf()),
    val discardPile: SnapshotStateList<Card> = mutableStateListOf(),
    val deck: SnapshotStateList<Card> = mutableStateListOf(),

    val skipCount: Int = 0,
    val soundOn: Boolean = true,

    val appDialogEvent: AppDialogEvent? = null,
    val gameDialogEvent: GameDialogEvent? = null,

    // increments on every turn/replay (used for effects/keys)
    val turnId: Int = 0,

    // 🔹 NEW unified hint string consumed by CommentsView
    val turnHint: String = "",

    // Used to display info in turnHint
    val lastActivePLayer: Player? = null,

    // The last kept card (the one kept by the lastActivePLayer
    val lastKeptCard: Card? = null
) {
    override fun toString(): String {
        return """
            🔍 GameState Debug Info:
            💠 Player ID: $playerId
            💠 Turn Info: $turnInfo
            💠 Do Not Auto Play AI: $doNotAutoPlayAI
            💠 Point Limit: $pointLimit
            💠 Nb of players: ${players.size}
            💠 Starting Player Index: $startingPlayerIndex
            💠 Current Player Index: $currentPlayerIndex
            💠 Skip Count: $skipCount
            💠 Current Combination on Mat: ${currentCombinationOnMat ?: "None"}
            💠 Discard Pile: ${discardPile.joinToString(", ") { it.toString() }}
            💠 Deck: ${deck.joinToString(", ") { it.toString() }}
            💠 Turn Hint: $turnHint
            💠 Last Active Player: ${lastActivePLayer?.name ?: "None"}
            💠 Turn ID : $turnId
        """.trimIndent()
    }

    fun deepCopy(): GameState {
        return GameState(
            playerId = this.playerId,
            turnInfo = this.turnInfo.copy(),
            doNotAutoPlayAI = this.doNotAutoPlayAI,
            pointLimit = this.pointLimit,

            players = this.players.map { it.copy() },
            startingPlayerIndex = this.startingPlayerIndex,
            currentPlayerIndex = this.currentPlayerIndex,

            currentCombinationOnMat = Combination(this.currentCombinationOnMat.cards.toMutableList()),
            discardPile = mutableStateListOf<Card>().apply { addAll(this@GameState.discardPile) },
            deck = mutableStateListOf<Card>().apply { addAll(this@GameState.deck) },

            skipCount = this.skipCount,
            soundOn = this.soundOn,

            appDialogEvent = this.appDialogEvent,
            gameDialogEvent = this.gameDialogEvent,

            turnId = this.turnId,

            turnHint = this.turnHint,
            lastActivePLayer = this.lastActivePLayer?.copy(),
            lastKeptCard = this.lastKeptCard?.copy()
        )
    }
}
