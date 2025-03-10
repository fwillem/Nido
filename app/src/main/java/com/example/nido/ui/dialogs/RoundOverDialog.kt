package com.example.nido.ui.dialogs

import android.R
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview // 🚀 Added Preview import
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.events.AppEvent
import com.example.nido.game.GameManager
import com.example.nido.data.model.Player
import com.example.nido.data.model.Hand
import com.example.nido.data.model.PlayerType
import com.example.nido.data.model.PlayerAction
import com.example.nido.data.model.PlayerActionType
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.layout.Column
import com.example.nido.ui.theme.NidoColors

@Composable
fun RoundOverDialog(event: AppEvent.GameEvent.RoundOver) { // 🚀 Updated to use playersHandScore as List<Pair<Player, Int>>
    AlertDialog(
        onDismissRequest = { GameManager.clearDialogEvent() },
        title = {
            Text("${event.winner.name} won this round!")
        },
        text = {
            Column {
                // 🚀 Display each player's score from the list of pairs
                event.playersHandScore.forEach { (player, score) ->
                    Text(text = "${player.name}: $score", fontSize = 16.sp)

                }
            }
        },
        confirmButton = {
            Button(
                onClick = { GameManager.clearDialogEvent() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Gray
                )
            ) {
                Text(
                    "OK",
                    fontSize = 24.sp,
                    color = NidoColors.SecondaryText
                )
            }
        },
        containerColor = Color.White.copy(alpha = 0.7f)
    )
}

// 🚀 Dummy implementations of Player for preview purposes.
private
val dummyLocalPlayer = object : Player {
    override val id: String = "1"
    override val name: String = "Alice"
    override val avatar: String = ""
    override val playerType: PlayerType = PlayerType.LOCAL
    override var score: Int = 80
    override val hand: Hand = Hand(mutableStateListOf())
    override fun play(gameManager: com.example.nido.game.GameManager): PlayerAction {
        return PlayerAction(PlayerActionType.SKIP)
    }

    override fun copy(
        id: String,
        name: String,
        avatar: String,
        score: Int,
        hand: Hand
    ): Player = this
}

private
val dummyAIPlayer1 = object : Player {
    override val id: String = "2"
    override val name: String = "Bob"
    override val avatar: String = ""
    override val playerType: PlayerType = PlayerType.AI
    override var score: Int = 60
    override val hand: Hand = Hand(mutableStateListOf())
    override fun play(gameManager: com.example.nido.game.GameManager): PlayerAction {
        return PlayerAction(PlayerActionType.SKIP)
    }

    override fun copy(
        id: String,
        name: String,
        avatar: String,
        score: Int,
        hand: Hand
    ): Player = this
}

private
val dummyAIPlayer2 = object : Player {
    override val id: String = "3"
    override val name: String = "Carol"
    override val avatar: String = ""
    override val playerType: PlayerType = PlayerType.AI
    override var score: Int = 70
    override val hand: Hand = Hand(mutableStateListOf())
    override fun play(gameManager: com.example.nido.game.GameManager): PlayerAction {
        return PlayerAction(PlayerActionType.SKIP)
    }

    override fun copy(
        id: String,
        name: String,
        avatar: String,
        score: Int,
        hand: Hand
    ): Player = this
}

// 🚀 Preview for RoundOverDialog in landscape mode with three players.
@Preview(
    name = "RoundOverDialog Preview - Landscape",
    showBackground = true,
    widthDp = 800,  // 🚀 Landscape width
    heightDp = 400  // 🚀 Landscape height
)
@Composable
fun PreviewRoundOverDialog() {
    // 🚀 Create a dummy RoundOver event with a sample playersHandScore list including three players.
    val dummyEvent = AppEvent.GameEvent.RoundOver(
        winner = dummyLocalPlayer,
        playersHandScore = listOf(
            dummyLocalPlayer to 80,  // 🚀 Alice (local)
            dummyAIPlayer1 to 60,     // 🚀 Bob (AI)
            dummyAIPlayer2 to 70      // 🚀 Carol (AI)
        )
    )
    RoundOverDialog(event = dummyEvent)
}
