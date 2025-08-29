package com.example.nido.data.model

import com.example.nido.game.IGameManager
import com.example.nido.data.model.Combination

/**
 * Enum representing the type of action a player can take.
 */
enum class PlayerActionType {
    PLAY, // The player is playing a combination.
    SKIP  // The player is choosing to skip their turn.
}

/**
 * Data class that encapsulates the result of a player's action.
 */
data class PlayerAction(
    val actionType: PlayerActionType,
    val combination: Combination? = null,
    val cardToKeep: Card? = null,
    val comment: String? = null
)

/**
 * The Player interface represents a participant in the game.
 */
interface Player {
    val id: String
    val name: String
    val avatar: String
    val playerType: PlayerType
    var score: Int
    val hand: Hand


    /**
     * Called when it's the player's turn.
     *
     * ✅ Updated to reference IGameManager instead of GameManager
     */
    fun play(gameManager: IGameManager): PlayerAction

    /**
     * Returns a copy of the player, optionally replacing some properties.
     */
    fun copy(
        id: String = this.id,
        name: String = this.name,
        avatar: String = this.avatar,
        score: Int = this.score,
        hand: Hand = this.hand,
    ): Player
}
