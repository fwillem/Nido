// GameState.kt  (FULL — replace file)
package com.example.nido.game

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.events.AppDialogEvent
import com.example.nido.events.GameDialogEvent
import com.example.nido.game.multiplayer.MultiplayerState
import com.example.nido.replay.GameSession
import com.example.nido.utils.Constants
import java.util.UUID


data class GameState(
    val turnInfo: TurnInfo = TurnInfo(),
    val doNotAutoPlayAI: Boolean = false,
    val pointLimit: Int = Constants.GAME_DEFAULT_POINT_LIMIT,
    val aiTimerDuration: Int = Constants.AI_THINKING_DURATION_DEFAULT,

    val players: List<Player> = emptyList(),
    val startingPlayerIndex: Int = 0,
    val currentPlayerIndex: Int = 0,
    val currentPlayerId: String = "",
    val multiplayerState: MultiplayerState? = null,

    val currentCombinationOnMat: Combination = Combination(mutableListOf()),
    val discardPile: SnapshotStateList<Card> = mutableStateListOf(),
    val deck: SnapshotStateList<Card> = mutableStateListOf(),

    val skipCount: Int = 0,

    val appDialogEvent: AppDialogEvent? = null,
    val gameDialogEvent: GameDialogEvent? = null,

    // increments on every turn/replay (used for effects/keys)
    val turnId: Int = 0,

    // 🔹 NEW unified hint string consumed by CommentsView
    val turnHintMsg: TurnHintMsg? = null,
    val bannerMsg: BannerMsg? = null,

    // Queue of transient UI notices (snackbar/banner); UI consumes and removes them
    val pendingNotices: List<UiNotice> = emptyList(),


// Used to display info in turnHint
    val lastPlayerWhoPlayed: Player? = null, // Used to know who played the cards that are currently on the mat
    val lastPlayerWhoSkipped: Player? = null, // Used to provide hints to the player

    // The last kept card (the one kept by the lastActivePLayer)
    val lastKeptCard: Card? = null,

    // History of games
    val sessions: MutableList<GameSession> = mutableListOf(),  // history of all games
    val currentSession: GameSession? = null,                    // active session

    // Sounds & Music
    val soundEffectVolume : SoundVolume = SoundVolume.Medium,
    val soundMusicVolume : SoundVolume = SoundVolume.Off,
    val pendingSounds: List<SoundEffect> = emptyList(),
    val pendingMusic: List<MusicCommand> = emptyList(),



    ) {
    override fun toString(): String {
        return """
            🔍 GameState Debug Info:
            💠 Turn Info: $turnInfo
            💠 Do Not Auto Play AI: $doNotAutoPlayAI
            💠 Point Limit: $pointLimit
            💠 AI Timer Duration: $aiTimerDuration
            💠 Nb of players: ${players.size}
            💠 Starting Player Index: $startingPlayerIndex
            💠 Current Player Index: $currentPlayerIndex
            💠 Current Player ID: $currentPlayerId
            💠 MultiPlayer State: $multiplayerState
            💠 Skip Count: $skipCount
            💠 Current Combination on Mat: $currentCombinationOnMat
            💠 Discard Pile: ${discardPile.joinToString(", ") { it.toString() }}
            💠 Deck: ${deck.joinToString(", ") { it.toString() }}
            💠 Turn Hint: ${turnHintMsg ?: "None"}
            💠 Banner Message: ${bannerMsg ?: "None"}
            💠 Pending Notices: ${if (pendingNotices.isEmpty()) "None" else pendingNotices.joinToString(", ")}
            💠 Last Player Who Played: ${lastPlayerWhoPlayed?.name ?: "None"}
            💠 Last Player Who Skipped: ${lastPlayerWhoSkipped?.name ?: "None"}
            💠 Turn ID : $turnId
            💠 Sound Effect Volume: $soundEffectVolume
            💠 Sound Music Volume: $soundMusicVolume
            💠 Pending Sounds: ${if (pendingSounds.isEmpty()) "None" else pendingSounds.joinToString(", ")}
            💠 Pending Music Commands: ${if (pendingMusic.isEmpty()) "None" else pendingMusic.joinToString(", ")}
        """.trimIndent()
    }

    fun deepCopy(): GameState {
        return GameState(
            turnInfo = this.turnInfo.copy(),
            doNotAutoPlayAI = this.doNotAutoPlayAI,

            pointLimit = this.pointLimit,
            aiTimerDuration = this.aiTimerDuration,

            players = this.players.map { it.copy() },
            startingPlayerIndex = this.startingPlayerIndex,
            currentPlayerIndex = this.currentPlayerIndex,
            currentPlayerId = this.currentPlayerId,
            multiplayerState = this.multiplayerState,

            currentCombinationOnMat = Combination(this.currentCombinationOnMat.cards.toMutableList()),
            discardPile = mutableStateListOf<Card>().apply { addAll(this@GameState.discardPile) },
            deck = mutableStateListOf<Card>().apply { addAll(this@GameState.deck) },

            skipCount = this.skipCount,
            soundEffectVolume = this.soundEffectVolume,
            soundMusicVolume = this.soundMusicVolume,
            pendingSounds = this.pendingSounds.toList(),
            pendingMusic = this.pendingMusic.toList(),

            appDialogEvent = this.appDialogEvent,
            gameDialogEvent = this.gameDialogEvent,

            turnId = this.turnId,

            turnHintMsg = this.turnHintMsg,
            bannerMsg = this.bannerMsg,
            pendingNotices = this.pendingNotices.toList(),

            lastPlayerWhoPlayed = this.lastPlayerWhoPlayed?.copy(),
            lastPlayerWhoSkipped = this.lastPlayerWhoSkipped?.copy(),
            lastKeptCard = this.lastKeptCard?.copy()
        )
    }
}

private fun String.ellipsize(max: Int): String =
    if (length <= max) this else take(max - 1) + "…"

/**
 * FULL (but focused) debug string for multiplayer work:
 * only the 4 requested signals.
 */
fun GameState.debugString(): String {
    val hintStr = turnHintMsg?.toString()?.ellipsize(80) ?: "-"
    val bannerStr = bannerMsg?.toString()?.ellipsize(80) ?: "-"

    val matCards = currentCombinationOnMat.cards
    val matStr = if (matCards.isEmpty()) {
        "mat=∅"
    } else {
        // Default: show count + a short preview. Adapt cardDebug() if needed.
        val preview = matCards.take(5).joinToString(separator = ",") { it.cardDebug() }
        val more = if (matCards.size > 5) " +${matCards.size - 5}" else ""
        "mat=${matCards.size}[$preview$more]"
    }

    val noticesStr = if (pendingNotices.isEmpty()) {
        "notices=∅"
    } else {
        val preview = pendingNotices.take(3).joinToString(separator = " | ") { it.noticeDebug() }
        val more = if (pendingNotices.size > 3) " +${pendingNotices.size - 3}" else ""
        "notices=${pendingNotices.size}[$preview$more]"
    }

    return "GS hint=\"$hintStr\" banner=\"$bannerStr\" $matStr $noticesStr"
}

/**
 * Very small representation of a card for logs.
 * Adjust this to match your Card model (rank/value/suit).
 */
private fun com.example.nido.data.model.Card.cardDebug(): String {
    // If Card.toString() is already compact, keep it.
    // Otherwise replace with something like "${rank}${suit}".
    return this.toString().ellipsize(12)
}

/**
 * Compact representation of a UI notice for logs.
 */
private fun UiNotice.noticeDebug(): String {
    val msg = message.ellipsize(40)
    val act = actionLabel?.let { ", act=${it.ellipsize(12)}" } ?: ""
    return "${kind.name}(${msg}$act)"
}

// ─────────────────────────────────────────────────────────────────────────────
// Lightweight notice model for transient UI messages.
// Kept in the game module because GameManager is the single source of truth.
// ─────────────────────────────────────────────────────────────────────────────
enum class NoticeKind { Info, Success, Warning, Error }

data class UiNotice(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val kind: NoticeKind = NoticeKind.Info,
    val actionLabel: String? = null
)
