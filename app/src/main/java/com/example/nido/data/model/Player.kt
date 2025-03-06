package com.example.nido.data.model

import com.example.nido.game.GameManager
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
 *
 * @property actionType Indicates whether the player is playing or skipping.
 * @property combination The combination played by the player (if any).
 * @property cardToKeep The card the player chooses to keep (if applicable).
 * @property comment An optional comment provided by the player.
 */
data class PlayerAction(
    val actionType: PlayerActionType,
    val combination: Combination? = null,
    val cardToKeep: Card? = null,
    val comment: String? = null
)

/**
 * The Player interface represents a participant in the game.
 *
 * Each player has properties such as id, name, avatar, player type, score, and a hand of cards.
 * The play() method returns a PlayerAction that encapsulates the player's move for the turn.
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
     * @param gameManager The current game manager instance for accessing game state or helper functions.
     * @return A PlayerAction instance describing what the player did, including the combination played,
     *         the card to keep, and an optional comment.
     */
    fun play(gameManager: GameManager): PlayerAction

    /**
     * Returns a copy of the player, optionally replacing some properties.
     */
    fun copy(
        id: String = this.id,
        name: String = this.name,
        avatar: String = this.avatar,
        score: Int = this.score,
        hand: Hand = this.hand
    ): Player
}
