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
    fun consumeMusic(cmd: MusicCommand)
    fun consumeNotice(notice: UiNotice)

    // 🟢 Network related actions
    /** Send a lightweight chat/ping message to a specific remote player. */
    fun chatWithRemotePlayer(remotePlayerId: String, text: String)

    /** Convenience: ping a predefined test peer if available (loopback scenario). */
    fun pingTestPeerIfPossible()
}