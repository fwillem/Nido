package com.example.nido.ui.components

import android.media.MediaPlayer
import androidx.annotation.RawRes
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.nido.R
import com.example.nido.game.MusicCommand
import com.example.nido.game.MusicTrack
import com.example.nido.game.SoundVolume

/**
 * Minimal background music handler using MediaPlayer.
 * - Keeps a single player for the active track.
 * - Applies loop flag and volume.
 * - Always consumes commands to keep queue clean.
 */
@Composable
fun MusicSideEffectHandler(
    pending: List<MusicCommand>,
    volume: SoundVolume,
    onConsumed: (MusicCommand) -> Unit
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var loadedTrack by remember { mutableStateOf<MusicTrack?>(null) }

    // Cleanup when composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            player?.stop()
            player?.release()
            player = null
            loadedTrack = null
        }
    }

    // React to commands and volume changes
    LaunchedEffect(pending, volume) {
        val gain = when (volume) {
            SoundVolume.off    -> 0f
            SoundVolume.Low    -> 0.25f
            SoundVolume.Medium -> 0.6f
            SoundVolume.High   -> 1.0f
        }

        // If volume is off, we stop any playing music (explicit behavior)
        if (gain <= 0f) {
            if (player?.isPlaying == true) {
                player?.stop()
            }
            player?.release()
            player = null
            loadedTrack = null
            // Still consume all commands to keep queue consistent
            pending.forEach { onConsumed(it) }
            return@LaunchedEffect
        }

        pending.forEach { cmd ->
            when (cmd) {
                is MusicCommand.Play -> {
                    val resId = musicResFor(cmd.track)
                    if (resId != null) {
                        // Rebuild player if track changed or player missing
                        if (loadedTrack != cmd.track || player == null) {
                            player?.stop()
                            player?.release()
                            player = MediaPlayer.create(context, resId)
                            loadedTrack = cmd.track
                        }
                        player?.isLooping = cmd.loop
                        player?.setVolume(gain, gain)
                        if (player?.isPlaying != true) player?.start()
                    }
                    onConsumed(cmd)
                }
                MusicCommand.Stop -> {
                    if (player?.isPlaying == true) {
                        player?.stop()
                    }
                    player?.release()
                    player = null
                    loadedTrack = null
                    onConsumed(cmd)
                }
            }
        }
    }
}

/** Map logical tracks to raw resources. Swap to real files when you add them. */
@RawRes
private fun musicResFor(track: MusicTrack): Int? = when (track) {
    MusicTrack.Menu    -> R.raw.start_game     // placeholder until you add a real loop
    MusicTrack.InGame  -> R.raw.bgm_deep_house           // placeholder loop-ish
    MusicTrack.Victory -> R.raw.game_over      // celebratory you already have
    MusicTrack.Defeat  -> R.raw.error          // "sad" fallback
}
