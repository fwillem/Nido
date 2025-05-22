
package com.example.nido.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.nido.ui.LocalGameManager


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
