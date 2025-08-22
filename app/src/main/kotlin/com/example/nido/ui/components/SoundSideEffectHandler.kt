package com.example.nido.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.nido.game.SoundEffect
import com.example.nido.game.SoundVolume
import com.example.nido.R

/**
 * Centralized sound handler for side effects coming from GameManager.
 * It observes the pending sounds in GameState and plays them using SoundPool.
 *
 * Lifecycle:
 * - Loads all sounds when the Composable is first composed
 * - Plays/consumes sounds when pending list changes
 * - Releases SoundPool when this composable leaves composition
 */
@Composable
fun SoundSideEffectHandler(
    pending: List<SoundEffect>,              // Sounds requested by GameManager
    volume: SoundVolume,                     // Current user preference (Low/Medium/High/Off)
    onConsumed: (SoundEffect) -> Unit        // Callback to remove the effect from GameState
) {
    val context = LocalContext.current

    // Remember SoundPool for the entire lifecycle of this composable
    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(4) // allow overlapping effects
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }

    // Map SoundEffect → soundId (loaded from res/raw)
    val soundIds = remember {
        loadSounds(context, soundPool)
    }

    // Cleanup when composable is disposed
    DisposableEffect(Unit) {
        onDispose { soundPool.release() }
    }

    // Play new pending sounds when list changes
    LaunchedEffect(pending) {
        if (volume == SoundVolume.off) return@LaunchedEffect

        for (effect in pending) {
            val id = soundIds[effect]
            if (id != null) {
                soundPool.play(
                    id,
                    volume.toFloat(), // left volume
                    volume.toFloat(), // right volume
                    1,                // priority
                    0,                // no loop
                    1f                // normal rate
                )
                onConsumed(effect) // notify GameManager → remove from pending list
            }
        }
    }
}

/**
 * Loads raw resources into SoundPool and returns a map of SoundEffect → soundId.
 */
private fun loadSounds(context: Context, pool: SoundPool): Map<SoundEffect, Int> {
    return mapOf(
        // 🎲 Game lifecycle
        SoundEffect.NewRound   to pool.load(context, R.raw.start_game,   1),
        SoundEffect.CardPlayed to pool.load(context, R.raw.play,    1),
        SoundEffect.Skip       to pool.load(context, R.raw.skip,    1),
        SoundEffect.TurnStart  to pool.load(context, R.raw.hum1,    1),
        SoundEffect.RoundOver  to pool.load(context, R.raw.gloop,   1),
        SoundEffect.GameOver   to pool.load(context, R.raw.game_over,   1),

        // 💡 Hints
        SoundEffect.CannotBeat     to pool.load(context, R.raw.error,    1),
        SoundEffect.MustPlayOne    to pool.load(context, R.raw.gloop2,   1),
        SoundEffect.MustPlayAllIn  to pool.load(context, R.raw.hum2,     1),
        SoundEffect.CanPlayChoice  to pool.load(context, R.raw.gloop,    1),
        SoundEffect.MatDiscarded   to pool.load(context, R.raw.discard1,  1),
        SoundEffect.YouKept        to pool.load(context, R.raw.play,     1),
        SoundEffect.PlayerKept     to pool.load(context, R.raw.play,     1),
        SoundEffect.PlayerSkippedHint to pool.load(context, R.raw.skip,  1)
    )
}

/**
 * Extension function to convert SoundVolume enum to float gain (0.0 – 1.0).
 */
private fun SoundVolume.toFloat(): Float = when (this) {
    SoundVolume.off -> 0f
    SoundVolume.Low -> 0.3f
    SoundVolume.Medium -> 0.6f
    SoundVolume.High -> 1f
}
