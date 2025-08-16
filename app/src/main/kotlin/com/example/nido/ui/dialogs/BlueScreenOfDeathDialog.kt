package com.example.nido.ui.dialogs

import com.example.nido.utils.TraceLogLevel
import com.example.nido.utils.getTag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun BlueScreenOfDeathDialog(
    level: TraceLogLevel,
    tag: String = getTag(),
    message: () -> String,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onExit() },
        title = {
            Text(text = "${level.name}: $tag")
        },
        text = {
            Text(text = message())
        },
        confirmButton = {
            TextButton(onClick = { onExit() }) {
                Text("OK")
            }
        },
        containerColor = Color.Red
    )
}

@NidoPreview(name = "BlueScreenOfDeathDialog")
@Composable
fun PreviewBlueScreenOfDeathDialog() {
    NidoTheme {
        val fakeGameManager = FakeGameManager()
        CompositionLocalProvider(LocalGameManager provides fakeGameManager) {
            BlueScreenOfDeathDialog(
                level = TraceLogLevel.ERROR,
                tag = "BSOD",
                message = { "Something went wrong" },
                onExit = { }
            )
        }
    }
}
