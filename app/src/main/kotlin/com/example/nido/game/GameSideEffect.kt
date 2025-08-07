package com.example.nido.game
import com.example.nido.events.AppEvent

sealed class GameSideEffect {
    data class StartAITimer(val turnId: Int) : GameSideEffect()
    data class ShowDialog(val dialog: AppEvent.GameEvent) : GameSideEffect()
    object GetAIMove : GameSideEffect()
}
