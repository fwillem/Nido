package com.example.nido.events

import com.example.nido.data.model.Card
import com.example.nido.data.model.Player
import com.example.nido.utils.TraceLogLevel
import com.example.nido.utils.getTag

sealed class DialogEvent {
    data class RoundOver(
        val winner: Player,
        val playersHandScore: List<Pair<Player, Int>>
    ) : DialogEvent()

    data class GameOver(
        val playerRankings: List<Pair<Player, Int>>
    ) : DialogEvent()

    data class CardSelection(
        val candidateCards: List<Card>,
        val selectedCards: List<Card>,
        val onConfirm: (Card) -> Unit,
        val onCancel: () -> Unit
    ) : DialogEvent()

    object QuitGame : DialogEvent()

    data class BlueScreenOfDeath(
        val level: TraceLogLevel,
        val tag: String ,
        val message: () -> String,
    ) : DialogEvent()

}

