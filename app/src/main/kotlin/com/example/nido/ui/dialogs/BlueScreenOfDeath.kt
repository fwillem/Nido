package com.example.nido.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.game.FakeGameManager
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.preview.NidoPreview
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.utils.TraceLogLevel
import com.example.nido.utils.getTag
import kotlinx.coroutines.delay

/**
 * Dialog shown in case of fatal error (TRACE(FATAL)).
 * Triggered by AppDialogEvent.BlueScreenOfDeath.
 */
@Composable
fun BlueScreenOfDeathDialog(
    tag: String = getTag(),
    message: () -> String,
    onExit: () -> Unit
) {
    val gameManager = LocalGameManager.current

    // Flashing RED/BLUE background and BLACK/WHITE text respectively
    var isRed by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            isRed = !isRed
        }
    }
    val bgColor = if (isRed) Color.Red else Color.Blue
    val textColor = if (isRed) Color.Black else Color.White

    AlertDialog(
        onDismissRequest = {
            gameManager.clearAppDialogEvent()
            onExit()
        },

        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "\uD83D\uDC80  [$tag]  \uD83D\uDC80",
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = message(), color = textColor, fontSize = 24.sp)
            }

        },
        confirmButton = {
            TextButton(onClick = {
                gameManager.clearAppDialogEvent()
                onExit()
                // Force-crash after showing BSOD if desired:
                throw RuntimeException(message())
            }) {
                Text("OK", color = textColor)
            }
        },
        containerColor = bgColor
    )
}

@NidoPreview(name = "BlueScreenOfDeathDialog")
@Composable
fun PreviewBlueScreenOfDeathDialog() {
    NidoTheme {
        val fakeGameManager = FakeGameManager()
        CompositionLocalProvider(LocalGameManager provides fakeGameManager) {
            BlueScreenOfDeathDialog(
                tag = "BSOD",
                message = { "Something went wrong" },
                onExit = { }
            )
        }
    }
}
