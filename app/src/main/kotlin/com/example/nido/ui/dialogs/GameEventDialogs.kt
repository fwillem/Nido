package com.example.nido.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.R
import com.example.nido.events.AppEvent
import com.example.nido.game.FakeGameManager
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.preview.NidoPreview
import com.example.nido.ui.theme.NidoTheme


@Composable
fun PlayerLeftDialog(event: AppEvent.PlayerEvent.PlayerLeft) { // 🚀 Extracted PlayerLeft dialog
    val gameManager = LocalGameManager.current  // ✅ Retrieve injected GameManager

    AlertDialog(
        onDismissRequest = { gameManager.clearDialogEvent() },
        title = { Text(stringResource(R.string.player_left)) },
        text = { Text(stringResource(R.string.has_left_the_game, event.player.name)) },
        confirmButton = {
            Button(
                onClick = { gameManager.clearDialogEvent() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Gray
                )
            ) {
                Text(stringResource(R.string.ok))
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


@NidoPreview(name = "GameOverDialog")
@Composable
fun PreviewGameEventDialog() {
    NidoTheme {
        val fakeGameManager = FakeGameManager()

        CompositionLocalProvider(LocalGameManager provides fakeGameManager) {
            // Example usage of PlayerLeftDialog, assuming a player has left
            // You might need to adjust the event creation based on your FakeGameManager setup
            val playerWhoLeft = fakeGameManager.gameState.value.players.firstOrNull() ?: return@CompositionLocalProvider // Ensure player exists
            PlayerLeftDialog(
                event = AppEvent.PlayerEvent.PlayerLeft(playerWhoLeft)
            )
        }
    }
}
