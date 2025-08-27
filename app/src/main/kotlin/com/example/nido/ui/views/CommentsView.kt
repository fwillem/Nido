package com.example.nido.ui.views

import com.example.nido.game.GameState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.R
import com.example.nido.game.TurnHintMsg
import com.example.nido.ui.LocalGameManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.nido.game.IGameManager
import com.example.nido.game.GameViewModel
import com.example.nido.data.model.Card
import com.example.nido.data.model.Player
import com.example.nido.data.model.Hand
import com.example.nido.events.AppDialogEvent
import com.example.nido.events.GameDialogEvent
import com.example.nido.game.MusicCommand
import com.example.nido.game.SoundEffect
import com.example.nido.game.UiNotice
import com.example.nido.game.multiplayer.MultiplayerState

@Composable
fun TurnHintText(state: GameState) {
    val text = when (val m = state.turnHintMsg) {
        is TurnHintMsg.PlayerSkipped         -> stringResource(R.string.hint_player_skipped, m.name)
        is TurnHintMsg.MatDiscardedNext      -> stringResource(R.string.hint_mat_discarded, m.name)
        is TurnHintMsg.YouCannotBeat         -> stringResource(R.string.hint_cannot_beat)
        is TurnHintMsg.YouMustPlayOne        -> stringResource(R.string.hint_must_play) +
                if (m.canAllIn) stringResource(R.string.hint_or_all_in) else ""
        is TurnHintMsg.YouCanPlayNOrNPlusOne -> stringResource(R.string.hint_can_play, m.n, m.n + 1)
        is TurnHintMsg.YouKept               -> stringResource(R.string.hint_you_kept, m.card)
        is TurnHintMsg.PlayerKept            -> stringResource(R.string.hint_player_kept, m.name, m.card)
        null                                -> ""
    }
    Text(text, fontWeight = FontWeight.Bold, color = Color.White)
}

@Composable
fun CommentsView(actions: Map<String, () -> Unit>) {
    val gameManager = LocalGameManager.current
    val gameState = gameManager.gameState.value

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Text zone - 80%
        Box(
            modifier = Modifier.weight(0.99f, fill = true),
            contentAlignment = Alignment.Center
        ) {
            TurnHintText(gameState)
        }

        // Vertical Divider (Material3 only provides horizontal, but this works)
        VerticalDivider(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )


        // Buttons zone - 20%
        Row(
            modifier = Modifier.weight(0.01f, fill = true),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions.forEach { (label, action) ->
                Button(
                    onClick = action,
                    modifier = Modifier
                        .height(16.dp)
                        .padding(horizontal = 2.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text(
                        label,
                        fontSize = 8.sp,
                        lineHeight = 8.sp
                    )
                }
            }
        }


    }
}

@Preview(name = "Comments - YouMustPlayOne", showBackground = true)
@Composable
private fun CommentsViewPreview_YouMustPlayOne() {
    // Fake state with the desired hint
    val fakeStateFlow = MutableStateFlow(
        GameState(
            turnHintMsg = TurnHintMsg.YouMustPlayOne(canAllIn = true)
        )
    )

    // Lightweight fake GameManager just for preview
    val fakeManager = object : IGameManager {
        override val gameState: StateFlow<GameState> get() = fakeStateFlow
        override fun isGameOver() = false
        override fun getGameWinners(): List<Player> = emptyList()
        override fun getPlayerRankings(): List<Pair<Player, Int>> = emptyList()
        override fun getPlayerHandScores(): List<Pair<Player, Int>> = emptyList()
        override fun getCurrentPlayerHandSize(): Int = 0
        override fun isCurrentPlayerLocal(): Boolean = true
        override fun currentPlayerHasValidCombination(): Boolean = false
        override fun isValidMove(selectedCards: List<Card>): Boolean = false
        override fun hasPlayedAllRemainingCards(): Boolean = false

        override fun initialize(viewModel: GameViewModel) {}
        override fun startNewGame(selectedPlayers: List<Player>, selectedPointLimit: Int, doNotAutoPlayAI: Boolean, aiTimerDuration: Int) {}
        override fun startNewRound() {}
        override fun playCombination(selectedCards: List<Card>, cardToKeep: Card?) {}
        override fun skipTurn() {}
        override fun getAIMove() {}
        override fun processSkip() {}

        override fun updatePlayerHand(playerIndex: Int, hand: Hand) {}
        override fun setAppDialogEvent(event: AppDialogEvent) {}
        override fun clearAppDialogEvent() {}
        override fun setGameDialogEvent(event: GameDialogEvent) {}
        override fun clearGameDialogEvent() {}
        override fun consumeSound(effect: SoundEffect) {}
        override fun consumeMusic(cmd: MusicCommand) {}
        override fun consumeNotice(notice: UiNotice) {}

        override fun chatWithRemotePlayer(remotePlayerId: String, text: String) {}
        override fun pingTestPeerIfPossible() {}
        override fun setMultiplayerState(state: MultiplayerState?) {}
        override fun getMultiplayerState(): MultiplayerState? = null
        override fun hostQuickRoom(myUid: String) {}
        override fun joinQuickRoom(myUid: String) {}
        override fun autoQuickConnect(myUid: String) {}
    }

    CompositionLocalProvider(LocalGameManager provides fakeManager) {
        MaterialTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                CommentsView(actions = emptyMap())
            }
        }
    }
}

@Preview(name = "Comments - YouMustPlayOne (no all-in)", showBackground = true)
@Composable
private fun CommentsViewPreview_YouMustPlayOne_NoAllIn() {
    val fakeStateFlow = MutableStateFlow(
        GameState(
            turnHintMsg = TurnHintMsg.YouMustPlayOne(canAllIn = false)
        )
    )

    val fakeManager = object : IGameManager {
        override val gameState: StateFlow<GameState> get() = fakeStateFlow
        override fun isGameOver() = false
        override fun getGameWinners(): List<Player> = emptyList()
        override fun getPlayerRankings(): List<Pair<Player, Int>> = emptyList()
        override fun getPlayerHandScores(): List<Pair<Player, Int>> = emptyList()
        override fun getCurrentPlayerHandSize(): Int = 0
        override fun isCurrentPlayerLocal(): Boolean = true
        override fun currentPlayerHasValidCombination(): Boolean = false
        override fun isValidMove(selectedCards: List<Card>): Boolean = false
        override fun hasPlayedAllRemainingCards(): Boolean = false

        override fun initialize(viewModel: GameViewModel) {}
        override fun startNewGame(selectedPlayers: List<Player>, selectedPointLimit: Int, doNotAutoPlayAI: Boolean, aiTimerDuration: Int) {}
        override fun startNewRound() {}
        override fun playCombination(selectedCards: List<Card>, cardToKeep: Card?) {}
        override fun skipTurn() {}
        override fun getAIMove() {}
        override fun processSkip() {}

        override fun updatePlayerHand(playerIndex: Int, hand: Hand) {}
        override fun setAppDialogEvent(event: AppDialogEvent) {}
        override fun clearAppDialogEvent() {}
        override fun setGameDialogEvent(event: GameDialogEvent) {}
        override fun clearGameDialogEvent() {}
        override fun consumeSound(effect: SoundEffect) {}
        override fun consumeMusic(cmd: MusicCommand) {}
        override fun consumeNotice(notice: UiNotice) {}
        override fun chatWithRemotePlayer(remotePlayerId: String, text: String) {}
        override fun pingTestPeerIfPossible() {}
        override fun setMultiplayerState(state: MultiplayerState?) {}
        override fun getMultiplayerState(): MultiplayerState? = null
        override fun hostQuickRoom(myUid: String) {}
        override fun joinQuickRoom(myUid: String) {}
        override fun autoQuickConnect(myUid: String) {}
    }

    CompositionLocalProvider(LocalGameManager provides fakeManager) {
        MaterialTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                CommentsView(actions = emptyMap())
            }
        }
    }
}
