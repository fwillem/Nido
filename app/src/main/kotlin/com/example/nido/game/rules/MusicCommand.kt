package com.example.nido.game

/** One-shot commands consumed by the UI music handler. */
sealed class MusicCommand {
    data class Play(val track: MusicTrack, val loop: Boolean = true) : MusicCommand()
    data object Stop : MusicCommand()
}
