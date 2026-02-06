package com.example.nido.game

import com.example.nido.data.model.Card
import com.example.nido.game.BannerMsg
import com.example.nido.game.TurnHintMsg
sealed class GameEvent() {
    object NewRoundStarted : GameEvent()
    data class CardPlayed(
        val playerId: String,
        val playedCards: List<Card>,
        val cardKept: Card? = null
    ) : GameEvent()
    object PlayerSkipped : GameEvent()

    object NextTurn : GameEvent()
    data class RoundOver(val localPlayerWon: Boolean) : GameEvent()
    data class GameOver(val localPlayerWon: Boolean) : GameEvent()
    data class AITimerExpired(val turnId: Int) : GameEvent()
    object QuitGame : GameEvent()

    // Network Events (Guest side)
    data class NetworkStateSync(
        val snapshot: GameSerializer.ClientSnapshot
    ) : GameEvent()

    data class TurnHintReceived(val hint: TurnHintMsg) : GameEvent()
    data class BannerReceived(val banner: BannerMsg) : GameEvent()
    
    // Add additional events as needed.
}

private fun String.shortId(len: Int = 6): String =
    if (length <= len) this else take(len)

/**
 * Compact, stable debug string for GameEvent.
 * Designed for logs (reducers, network traces, replay).
 */
fun GameEvent.debugString(): String = when (this) {

    // --- Core game flow -----------------------------------------------------

    GameEvent.NewRoundStarted ->
        "NewRoundStarted"

    is GameEvent.CardPlayed ->
        "CardPlayed(p=${playerId.shortId()}, cards=${playedCards.size}, kept=${cardKept != null})"

    GameEvent.PlayerSkipped ->
        "PlayerSkipped"

    GameEvent.NextTurn ->
        "NextTurn"

    is GameEvent.RoundOver ->
        "RoundOver(win=$localPlayerWon)"

    is GameEvent.GameOver ->
        "GameOver(win=$localPlayerWon)"

    is GameEvent.AITimerExpired ->
        "AITimerExpired(turn=$turnId)"

    GameEvent.QuitGame ->
        "QuitGame"

    // --- Network / multiplayer ---------------------------------------------

    is GameEvent.NetworkStateSync ->
        "NetworkStateSync"

    // --- UI / feedback ------------------------------------------------------

    is GameEvent.TurnHintReceived ->
        "TurnHintReceived"

    is GameEvent.BannerReceived ->
        "BannerReceived"
}
