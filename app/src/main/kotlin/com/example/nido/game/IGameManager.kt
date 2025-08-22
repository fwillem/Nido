package com.example.nido.game

import com.example.nido.data.model.Hand
import com.example.nido.events.AppDialogEvent
import com.example.nido.events.GameDialogEvent


interface IGameManager : IGameStateProvider, IGameActions {
    fun updatePlayerHand(playerIndex: Int, hand: Hand)
    fun setAppDialogEvent(event: AppDialogEvent)
    fun clearAppDialogEvent()

    fun setGameDialogEvent(event: GameDialogEvent)
    fun clearGameDialogEvent()
    /** Consume a one-shot sound so it won’t replay on recomposition. */
    fun consumeSound(effect: SoundEffect)
}
