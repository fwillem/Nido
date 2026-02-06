package com.example.nido.game

import com.example.nido.events.GameDialogEvent

sealed class GameSideEffect {
    data class StartAITimer(val turnId: Int) : GameSideEffect()
    data class ShowDialog(val dialog: GameDialogEvent) : GameSideEffect()
    object GetAIMove : GameSideEffect()
    data class PlaySound(val effect: SoundEffect) : GameSideEffect()
    data class PlayMusic(val track: MusicTrack, val loop: Boolean = true) : GameSideEffect()
    data object StopMusic : GameSideEffect()
}

/**
 * FULL but controlled debug string for GameSideEffect.
 */
fun GameSideEffect.debugString(): String = when (this) {

    is GameSideEffect.StartAITimer ->
        "StartAITimer(turn=$turnId)"

    GameSideEffect.GetAIMove ->
        "GetAIMove"

    is GameSideEffect.ShowDialog ->
        "ShowDialog(type=${dialog::class.simpleName ?: "?"})"

    is GameSideEffect.PlaySound ->
        "PlaySound(effect=${effect::class.simpleName ?: effect.toString()})"

    is GameSideEffect.PlayMusic ->
        "PlayMusic(track=${track::class.simpleName ?: track.toString()} loop=$loop)"

    GameSideEffect.StopMusic ->
        "StopMusic"
}
