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