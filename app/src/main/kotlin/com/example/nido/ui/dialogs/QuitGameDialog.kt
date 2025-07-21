
package com.example.nido.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.nido.game.LocalPlayer
import com.example.nido.game.ai.AIPlayer
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.preview.NidoPreview
import com.example.nido.ui.screens.SetupScreen
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.utils.Constants.GAME_DEFAULT_POINT_LIMIT


@Composable
fun QuitGameDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val gameManager = LocalGameManager.current  // ✅ Retrieve injected GameManager

    AlertDialog(
        onDismissRequest = {
            gameManager.clearDialogEvent()
            onCancel()
        },
        title = { Text("Quit game?") },
        text = { Text("Do you really want to quit ?") },
        confirmButton = {
            TextButton(onClick = {
                gameManager.clearDialogEvent()
                onConfirm()
            }) {
                Text("YES")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                gameManager.clearDialogEvent()
                onCancel()
            }) {
                Text("CANCEL")
            }
        }
    )
}

// @Preview(showBackground = true)
@NidoPreview(name = "QuitGameDialog")
@Composable
fun QuitGameDialogPreview() {
    NidoTheme {
        QuitGameDialog(
            onConfirm = {},
            onCancel = {})
    }
}