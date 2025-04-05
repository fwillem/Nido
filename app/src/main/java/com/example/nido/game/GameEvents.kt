package com.example.nido.game.events



sealed class GameEvent {
    object GameStarted : GameEvent()
    object CardsDealt : GameEvent()
    object NewRoundStarted : GameEvent()
    object PlayerTurnStarted : GameEvent()
    object CardPlayed : GameEvent()
}
