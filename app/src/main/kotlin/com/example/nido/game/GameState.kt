package com.example.nido.game

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.utils.Constants
import com.example.nido.events.DialogEvent

data class GameState(
    val playerId: String = "",
    val turnInfo: TurnInfo = TurnInfo(),
    val doNotAutoPlayAI: Boolean = false,
    val comment: String = "Nido",
    val pointLimit: Int = Constants.GAME_DEFAULT_POINT_LIMIT,
    val players: List<Player> = emptyList(),
    val startingPlayerIndex: Int = 0,
    val currentPlayerIndex: Int = 0,
    val currentCombinationOnMat: Combination = Combination(mutableListOf()),
    val discardPile: SnapshotStateList<Card> = mutableStateListOf(),
    val deck: SnapshotStateList<Card> = mutableStateListOf(), // Added deck
    val skipCount: Int = 0, // New property to track consecutive skips.
    val soundOn: Boolean = true,
    val dialogEvent: DialogEvent? = null,
    val turnId: Int = 0 // increments on every turn/replay. Allow to trigger launcheffect for automatic AI playing

) {
    override fun toString(): String {
        return """
            🔍 GameState Debug Info:
            💠 Player ID: $playerId
            💠 Turn Info: $turnInfo
            💠 Do Not Auto Play AI: $doNotAutoPlayAI
            💠 Comment: $comment
            💠 Point Limit: $pointLimit
            💠 Nb of players: ${players.size}
            💠 Starting Player Index: $startingPlayerIndex
            💠 Current Player Index: $currentPlayerIndex
            💠 Skip Count: $skipCount
            💠 Current Combination on Mat: ${currentCombinationOnMat ?: "None"}
            💠 Discard Pile: ${discardPile.joinToString(", ") { it.toString() }}
            💠 Deck: ${deck.joinToString(", ") { it.toString() }}
            💠 Sound On: $soundOn
            💠 Game Event: $dialogEvent
            💠 Turn ID : $turnId

        """.trimIndent()
    }

    fun deepCopy(): GameState {
        return GameState(

            playerId = this.playerId,
            turnInfo = this.turnInfo.copy(),
            doNotAutoPlayAI = this.doNotAutoPlayAI,
            comment = this.comment,
            pointLimit = this.pointLimit,
            players = this.players.map { it.copy() }, // Deep copy players
            startingPlayerIndex = this.startingPlayerIndex,
            currentPlayerIndex = this.currentPlayerIndex,
            currentCombinationOnMat = Combination(this.currentCombinationOnMat.cards.toMutableList()),
            discardPile = mutableStateListOf<Card>().apply { addAll(this@GameState.discardPile) },
            deck = mutableStateListOf<Card>().apply { addAll(this@GameState.deck) },
            skipCount = this.skipCount,
            soundOn = this.soundOn,
            dialogEvent = this.dialogEvent,
            turnId = this.turnId
        )
    }
}

