package com.example.nido.ui.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import com.example.nido.ui.LocalGameManager // 🟠 changed: correct package for LocalGameManager
import com.example.nido.game.NoticeKind
import com.example.nido.game.UiNotice

/**
 * 🟠 changed: English comments
 * Simple host that shows the first pending notice as a Material3 Snackbar.
 * UI remains dumb: it calls GameManager.consumeNotice() after showing.
 */
@Composable
fun NoticeHost() { // 🟠 changed: English comment
    val gameManager = LocalGameManager.current
    val state by gameManager.gameState.collectAsState()
    val notices = state.pendingNotices

    val hostState = remember { SnackbarHostState() }

    // When the head of the queue changes, display it
    LaunchedEffect(notices.firstOrNull()?.id) {
        val n = notices.firstOrNull() ?: return@LaunchedEffect
        val duration = when (n.kind) {
            NoticeKind.Info, NoticeKind.Success -> SnackbarDuration.Short
            NoticeKind.Warning -> SnackbarDuration.Short
            NoticeKind.Error -> SnackbarDuration.Long
        }
        hostState.showSnackbar(
            message = n.message,
            actionLabel = n.actionLabel,
            withDismissAction = true,
            duration = duration
        )
        gameManager.consumeNotice(n) // consume after display
    }

    SnackbarHost(hostState = hostState) { data ->
        Snackbar(snackbarData = data)
    }
}
