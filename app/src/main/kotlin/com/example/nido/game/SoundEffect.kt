package com.example.nido.game

/**
 * Pure domain identifiers for short SFX.
 * 1 sound = 1 event (and 1 sound = 1 hint) for now.
 * UI will map these to res/raw files later.
 *
 */
enum class SoundVolume {
    off,
    Low,
    Medium,
    High
}
enum class SoundEffect {
    // Game events
    NewRound,
    CardPlayed,
    Skip,
    TurnStart,
    RoundOverWin,
    RoundOverLose,
    GameOverWin,
    GameOverLose,

    // TurnHintMsg (optional, enable later)
    CannotBeat,
    MustPlayOne,
    MustPlayAllIn,
    CanPlayChoice,
    MatDiscarded,
    YouKept,
    PlayerKept,
    PlayerSkippedHint
}
