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
import com.example.nido.ui.LocalGameManager


@Composable
fun PlayerLeftDialog(event: AppEvent.PlayerEvent.PlayerLeft) { // 🚀 Extracted PlayerLeft dialog
    val gameManager = LocalGameManager.current  // ✅ Retrieve injected GameManager

    AlertDialog(
        onDismissRequest = { gameManager.clearDialogEvent() },
        title = { Text("Player Left") },
        text = { Text("${event.player.name} has left the game.") },
        confirmButton = {
            Button(
                onClick = { gameManager.clearDialogEvent() },
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
    val gameManager = LocalGameManager.current  // ✅ Retrieve injected GameManager

    AlertDialog(
        onDismissRequest = { gameManager.clearDialogEvent() },
        title = { Text("New Chat Message") },
        text = { Text("${event.sender.name}: ${event.message}") },
        confirmButton = {
            Button(
                onClick = { gameManager.clearDialogEvent() },
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
