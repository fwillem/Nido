package com.example.nido.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.events.AppEvent
import com.example.nido.ui.LocalGameManager

@Composable
fun GameOverDialog(event: AppEvent.GameEvent.GameOver) {
    val gameManager = LocalGameManager.current

    AlertDialog(
        onDismissRequest = { gameManager.clearDialogEvent() },
        title = {
            // Display the name of the top-ranked player in the title.
            Text("Game Over, congrats ${event.playerRankings.first().first.name}")
        },
        text = {
            // Wrap the text in a Column so each ranking is on its own line.
            Column {
                event.playerRankings.forEachIndexed { index, playerRanking ->
                    Text(
                        "${index + 1}. ${playerRanking.first.name} - Score: ${playerRanking.second}",
                        fontSize = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { gameManager.clearDialogEvent() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Gray
                )
            ) {
                Text("OK", fontSize = 12.sp)
            }
        },
        containerColor = Color.White.copy(alpha = 0.7f)
    )
}
