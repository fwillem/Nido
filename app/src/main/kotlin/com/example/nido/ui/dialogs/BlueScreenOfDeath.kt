package com.example.nido.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.R
import com.example.nido.game.FakeGameManager
import com.example.nido.ui.LocalGameManager
import com.example.nido.ui.preview.NidoPreview
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.utils.getTag
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dialog shown in case of fatal error (TRACE(FATAL)).
 * Triggered by AppDialogEvent.BlueScreenOfDeath.
 *
 * IMPORTANT:
 * - This dialog is "dumb": it never mutates game/app state by itself.
 * - The caller decides what to do in onExit() (e.g., hardRestartApp, crash, etc.).
 */

val gitHubTests = "11:15"


@Composable
fun BlueScreenOfDeathDialog(
    tag: String = getTag(),
    message: () -> String,
    isTerrifying: Boolean = false,
    onExit: () -> Unit,
    onCopyReport: (() -> Unit)? = null // optional: ask the app to copy a richer report
) {
    // Cache message once to avoid recompute and ensure consistent text over recompositions.
    val msg = remember(tag) { message() }

    if (isTerrifying) {
        BlueScreenOfDeathFullScreen(
            tag = tag,
            message = { msg },
            onExit = onExit,
            typewriter = true,
            onCopyReport = onCopyReport
        )
    } else {
        BlueScreenOfDeathDialogCool(
            tag = tag,
            message = { msg },
            onExit = onExit
        )
    }
}

/**
 * Small alert-style version with flashing background.
 * UI-only; onExit is called and the caller decides what to do.
 */
@Composable
fun BlueScreenOfDeathDialogCool(
    tag: String = getTag(),
    message: () -> String,
    onExit: () -> Unit
) {
    // Flash RED/BLUE and invert text color. Purely visual.

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
        onDismissRequest = onExit,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "\uD83D\uDC80  [$tag]  \uD83D\uDC80",
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(48.dp))
                Text(text = message(), color = textColor, fontSize = 24.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onExit) { Text("OK", color = textColor) }
        },
        containerColor = bgColor
    )
}



/**
 * Full-screen BSOD with optional typewriter effect + "Copy details" with tactile/snackbar feedback.
 * Still UI-only; onExit is forwarded to the caller.
 *
 * If [onCopyReport] is provided, a second button "Copy detailed report" is shown.
 * That callback is invoked with no params; the app decides what to copy.
 */
@Composable
fun BlueScreenOfDeathFullScreen(
    tag: String,
    message: () -> String,
    onExit: () -> Unit,
    typewriter: Boolean = false,
    onCopyReport: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    val fullText = remember(tag) {
        "A problem has been detected and Nido has been shut down to prevent damage to your mind, your device and your soul" +
                "\n\nTAG: $tag\n\nMESSAGE: ${message()}\n\nPress OK to restart..."
    }
    val shownText by animateTypewriter(fullText, enabled = typewriter)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0000AA)), // classic BSOD blue
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = shownText,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(32.dp))

            // Snackbar host at the bottom for the copy feedback.
            Box(
                Modifier
                   // .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                SnackbarHost(hostState = snack, modifier = Modifier.padding(12.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Local "Copy details" — copies exactly what the dialog shows
                Button(
                    onClick = {
                        copyToClipboard(ctx, "Nido BSOD", fullText)
                        // Give immediate feedback so it doesn't feel like a no-op.
                        // haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { snack.showSnackbar("Report copied to clipboard") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    // Use a string resource if present; otherwise replace with a plain string
                    Text(stringResource(R.string.copy_details), color = Color(0xFF0000AA))
                }

                // Optional: app-provided rich copy (includes game state, etc.), still no restart
                if (onCopyReport != null) {
                    Button(
                        onClick = {
                            onCopyReport()
                            // Give immediate feedback so it doesn't feel like a no-op.
                            // haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch { snack.showSnackbar("Detailed report copied to clipboard") }
                                  },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)

                    ) {
                        Text("Copy detailed report", color = Color(0xFF0000AA))
                    }
                }

                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) { Text("OK", color = Color(0xFF0000AA)) }
            }
        }


    }
}

/**
 * Simple typewriter effect for fun; returns the progressively revealed text when enabled.
 * Pure UI concern; no state/business logic here.
 */
@Composable
private fun animateTypewriter(text: String, enabled: Boolean): State<String> {
    val state = remember { mutableStateOf(if (enabled) "" else text) }
    LaunchedEffect(text, enabled) {
        if (!enabled) {
            state.value = text
            return@LaunchedEffect
        }
        state.value = ""
        for (i in text.indices) {
            state.value = text.substring(0, i + 1)
            delay(10) // tweak speed here
        }
    }
    return state
}

/** Helper to copy details to clipboard (used by the BSOD full-screen). */
private fun copyToClipboard(ctx: Context, label: String, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}

/* ---------------------- Previews (UI-only) ---------------------- */

@NidoPreview(name = "BlueScreenOfDeathDialog")
@Composable
fun PreviewBlueScreenOfDeathDialog() {
    NidoTheme {
        val fakeGameManager = FakeGameManager()
        // Provide a fake GameManager for previews only; dialog itself does not use it.
        CompositionLocalProvider(LocalGameManager provides fakeGameManager) {
            BlueScreenOfDeathDialog(
                tag = "BSOD",
                message = { "Something went wrong" },
                onExit = { /* no-op in preview */ }
            )
        }
    }
}



@NidoPreview(name = "BlueScreenOfDeathFullScreen")
@Composable
fun PreviewBlueScreenOfDeathFullScreen() {
    NidoTheme {
        val fakeGameManager = FakeGameManager()
        CompositionLocalProvider(LocalGameManager provides fakeGameManager) {
            BlueScreenOfDeathFullScreen(
                tag = "BSOD",
                message = { "Something went wrong" },
                onExit = { /* no-op in preview */ },
                typewriter = true
            )
        }
    }
}
