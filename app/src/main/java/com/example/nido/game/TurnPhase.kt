package com.example.nido.game

sealed class TurnPhase {
    data class WaitingForLocal(val name: String) : TurnPhase()
    data class WaitingForAI(
        val name: String,
        val isAutomatic: Boolean // true = will play after timer, false = wait for user to trigger AI move
    ) : TurnPhase()
    data class WaitingForRemote(val name: String) : TurnPhase()
    object Idle : TurnPhase()
}
