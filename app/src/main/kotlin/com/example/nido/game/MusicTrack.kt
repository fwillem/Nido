// com/example/nido/game/MusicTrack.kt
package com.example.nido.game

/** Logical music identities; UI will map them to res/raw files. */
enum class MusicTrack {
    Menu,      // landing / setup
    InGame,    // during a round
    Victory,   // local player won the whole game
    Defeat     // local player lost the whole game
}
