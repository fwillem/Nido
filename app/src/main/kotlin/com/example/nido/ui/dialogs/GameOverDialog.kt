package com.example.nido.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.R
import com.example.nido.events.GameDialogEvent
import com.example.nido.game.FakeGameManager
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.preview.NidoPreview
import com.example.nido.ui.screens.ScoreScreen
import com.example.nido.ui.theme.NidoColors
import com.example.nido.ui.theme.NidoTheme

@Composable
fun GameOverDialog(event: GameDialogEvent.GameOver, onExit : () -> Unit) {
    val gameManager = LocalGameManager.current

    AlertDialog(
        onDismissRequest = { gameManager.clearGameDialogEvent() ; onExit()},
        title = {
            // Display the name of the top-ranked player in the title.
            Text(
                stringResource(
                    R.string.game_over_congrats,
                    event.playerRankings.first().first.name
                ))
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
            TextButton(
                onClick = { gameManager.clearGameDialogEvent(); onExit() }
            ) {
                Text("OK")
            }
        },
        containerColor = Color.White.copy(alpha = 0.7f)
    )
}

@NidoPreview(name = "GameOverDialog")
@Composable
fun PreviewGameOverDialog() {
    NidoTheme {
        val fakeGameManager = FakeGameManager() // Create a single instance to avoid inconsistencies
        val players = fakeGameManager.gameState.value.players // Retrieve players safely

        CompositionLocalProvider(LocalGameManager provides fakeGameManager) {
            GameOverDialog(
                event = GameDialogEvent.GameOver(
                    playerRankings = fakeGameManager.getPlayerRankings()
                ),
                onExit = {  }
            )
        }
    }
}
