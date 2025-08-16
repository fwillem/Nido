package com.example.nido.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.nido.R
import com.example.nido.events.GameDialogEvent

@Composable
fun GameOverDialog(event: GameDialogEvent.GameOver, onExit : () -> Unit) {
    AlertDialog(
        onDismissRequest = { onExit() },
        title = {
            Text(
                stringResource(
                    R.string.game_over_congrats,
                    event.playerRankings.first().first.name
                ))
        },
        text = { },
        confirmButton = {
            TextButton(
                onClick = { onExit() }
            ) {
                Text("OK")
            }
        },
        containerColor = Color.White.copy(alpha = 0.7f)
    )
}
