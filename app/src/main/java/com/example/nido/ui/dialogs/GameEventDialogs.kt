package com.example.nido.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.events.AppEvent
import com.example.nido.game.GameManager


@Composable
fun RoundOverDialog(event: AppEvent.GameEvent.RoundOver) { // 🚀 Extracted RoundOver dialog
    AlertDialog(
        onDismissRequest = { GameManager.clearDialogEvent() },
        title = {
            Card(modifier = Modifier.background(Color.White.copy(alpha = 0.7f))) {
                Text("Round Over")
            }
        },
        text = {
            Card(modifier = Modifier.background(Color.White)) {
                Text(
                    "Winner: ${event.winner.name}\n" +
                            "Old Score: ${event.oldScore}\n" +
                            "Points Added: ${event.pointsAdded}\n" +
                            "New Score: ${event.newScore}"
                )
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
                Text("OK")
            }
        },
        containerColor = Color.White.copy(alpha = 0.7f)
    )
}

@Composable
fun GameOverDialog(event: AppEvent.GameEvent.GameOver) { // 🚀 Extracted GameOver dialog
    AlertDialog(
        onDismissRequest = { GameManager.clearDialogEvent() },
        title = { Text("Game Over") },
        text = { Text("Winner: ${event.playerRankings.first()}") },
        confirmButton = {
            Button(
                onClick = { GameManager.clearDialogEvent() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Gray
                )
            ) {
                Text("OK")
            }
        },
        containerColor = Color.White.copy(alpha = 0.7f)
    )
}

@Composable
fun PlayerLeftDialog(event: AppEvent.PlayerEvent.PlayerLeft) { // 🚀 Extracted PlayerLeft dialog
    AlertDialog(
        onDismissRequest = { GameManager.clearDialogEvent() },
        title = { Text("Player Left") },
        text = { Text("${event.player.name} has left the game.") },
        confirmButton = {
            Button(
                onClick = { GameManager.clearDialogEvent() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Gray
                )
            ) {
                Text("OK")
            }
        },
        containerColor = Color.White.copy(alpha = 0.7f)
    )
}

@Composable
fun ChatMessageDialog(event: AppEvent.PlayerEvent.ChatMessage) { // 🚀 Extracted ChatMessage dialog
    AlertDialog(
        onDismissRequest = { GameManager.clearDialogEvent() },
        title = { Text("New Chat Message") },
        text = { Text("${event.sender.name}: ${event.message}") },
        confirmButton = {
            Button(
                onClick = { GameManager.clearDialogEvent() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Gray
                )
            ) {
                Text("OK")
            }
        },
        containerColor = Color.White.copy(alpha = 0.7f)
    )
}
