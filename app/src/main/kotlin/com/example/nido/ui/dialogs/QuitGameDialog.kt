
package com.example.nido.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.nido.R
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
            gameManager.clearGameDialogEvent()
            onCancel()
        },
        title = { Text(stringResource(R.string.quit_game)) },
        text = { Text(stringResource(R.string.do_you_really_want_to_quit)) },
        confirmButton = {
            TextButton(onClick = {
                gameManager.clearGameDialogEvent()
                onConfirm()
            }) {
                Text(stringResource(R.string.yes))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                gameManager.clearGameDialogEvent()
                onCancel()
            }) {
                Text(stringResource(R.string.cancel))
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