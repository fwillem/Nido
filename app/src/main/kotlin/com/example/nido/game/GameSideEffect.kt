package com.example.nido.game
import com.example.nido.events.DialogEvent

sealed class GameSideEffect {
    data class StartAITimer(val turnId: Int) : GameSideEffect()
    data class ShowDialog(val dialog: DialogEvent) : GameSideEffect()
    object GetAIMove : GameSideEffect()
}
