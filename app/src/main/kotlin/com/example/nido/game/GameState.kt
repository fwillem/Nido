// GameState.kt  (FULL — replace file)
package com.example.nido.game

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.nido.data.model.Card
import com.example.nido.data.model.Combination
import com.example.nido.data.model.Player
import com.example.nido.events.AppDialogEvent
import com.example.nido.events.GameDialogEvent
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
