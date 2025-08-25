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
