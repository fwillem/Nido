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
import com.example.nido.data.model.Player
import com.example.nido.data.model.Hand
import com.example.nido.data.model.PlayerType
import com.example.nido.data.model.PlayerAction
import com.example.nido.data.model.PlayerActionType
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.TextButton
import com.example.nido.ui.theme.NidoColors
import com.example.nido.game.LocalPlayer
import com.example.nido.game.ai.AIPlayer
import com.example.nido.game.FakeGameManager
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.preview.NidoPreview

@Composable
fun RoundOverDialog(event: AppEvent.GameEvent.RoundOver, onExit: () -> Unit) { // 🚀 Updated to use playersHandScore as List<Pair<Player, Int>>
    val gameManager = LocalGameManager.current

    AlertDialog(
        onDismissRequest = { gameManager.clearDialogEvent() ; onExit()},
        title = {
            Text("${event.winner.name} won this round !")
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
            TextButton(
                onClick = { gameManager.clearDialogEvent() ; onExit()}
            ) {
                Text("OK")
            }
        },
        containerColor = Color.White.copy(alpha = 0.7f)
    )
}




// 🚀 Dummy implementations of Player for preview purposes.

private val dummyLocalPlayer = LocalPlayer(
    id = "1",
    name = "Alice",
    avatar = "",
    score = 80,
    hand = Hand(mutableStateListOf())
)

private val dummyAIPlayer1 = AIPlayer(
    id = "2",
    name = "Bob",
    avatar = "",
    score = 60,
    hand = Hand(mutableStateListOf())
)

private val dummyAIPlayer2 = AIPlayer(
    id = "3",
    name = "Carol",
    avatar = "",
    score = 70,
    hand = Hand(mutableStateListOf())
)




//
@NidoPreview(name = "SetupScreen")
@Composable
fun PreviewRoundOverDialog() {
    val fakeGameManager = FakeGameManager()

    val dummyEvent = AppEvent.GameEvent.RoundOver(
        winner = fakeGameManager.gameState.value.players.first(), // ✅ Use FakeGameManager player
        playersHandScore = fakeGameManager.getPlayerHandScores()  // ✅ Use FakeGameManager data
    )

    RoundOverDialog(event = dummyEvent, onExit = {})
}