package com.example.nido.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.events.AppEvent
import com.example.nido.game.FakeGameManager
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.screens.ScoreScreen
import com.example.nido.ui.theme.NidoColors
import com.example.nido.ui.theme.NidoTheme

@Composable
fun GameOverDialog(event: AppEvent.GameEvent.GameOver, onExit : () -> Unit) {
    val gameManager = LocalGameManager.current

    AlertDialog(
        onDismissRequest = { gameManager.clearDialogEvent() ; onExit()},
        title = {
            // Display the name of the top-ranked player in the title.
            Text("Game Over....\n\n ... congrats ${event.playerRankings.first().first.name} !!")
        },
        text = {
            /*
            // Wrap the text in a Column so each ranking is on its own line.
            Column {
                event.playerRankings.forEachIndexed { index, playerRanking ->
                    Text(
                        "${index + 1}. ${playerRanking.first.name} - Score: ${playerRanking.second}",
                        fontSize = 16.sp
                    )
                }
            }

             */
        },
        confirmButton = {
            Button(
                onClick = { gameManager.clearDialogEvent(); onExit() },
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

@Preview(
    name = "GameOverDialog Preview",
    widthDp = 800, // Wider than it is tall
    heightDp = 400, // Adjust as needed
    showBackground = true
)
@Composable
fun PreviewGameOverDialog() {
    NidoTheme {
        val fakeGameManager = FakeGameManager() // Create a single instance to avoid inconsistencies
        val players = fakeGameManager.gameState.value.players // Retrieve players safely

        CompositionLocalProvider(LocalGameManager provides fakeGameManager) {
            GameOverDialog(
                event = AppEvent.GameEvent.GameOver(
                    playerRankings = fakeGameManager.getPlayerRankings()
                ),
                onExit = {  }
            )
        }
    }
}
