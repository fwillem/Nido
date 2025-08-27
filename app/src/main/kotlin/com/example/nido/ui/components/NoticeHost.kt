package com.example.nido.ui.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import com.example.nido.ui.LocalGameManager // 🟠 changed: correct package for LocalGameManager
import com.example.nido.game.NoticeKind
import com.example.nido.game.UiNotice
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel
import com.example.nido.utils.TraceLogLevel.WARNING
import kotlin.coroutines.cancellation.CancellationException

/**
 * 🟠 changed: English comments
 * Simple host that shows the first pending notice as a Material3 Snackbar.
 * UI remains dumb: it calls GameManager.consumeNotice() after showing.
 */

@Composable
fun NoticeHost() {
    val gameManager = LocalGameManager.current
    val state by gameManager.gameState.collectAsState()
    val notices = state.pendingNotices
    val hostState = remember { SnackbarHostState() }

    DisposableEffect(Unit) {
        TRACE(WARNING) { "NoticeHost: mounted" }
        onDispose { TRACE(WARNING) { "NoticeHost: disposed" } }
    }

    LaunchedEffect(notices.firstOrNull()?.id) {
        val n = notices.firstOrNull() ?: return@LaunchedEffect
        TRACE(WARNING) { "NoticeHost: will show id=${n.id} size=${notices.size}" }
        try {
            val res = hostState.showSnackbar(
                message = n.message,
                actionLabel = n.actionLabel,
                withDismissAction = true,
                duration = when (n.kind) {
                    NoticeKind.Info, NoticeKind.Success -> SnackbarDuration.Short
                    NoticeKind.Warning -> SnackbarDuration.Short
                    NoticeKind.Error -> SnackbarDuration.Long
                }
            )
            TRACE(WARNING) { "NoticeHost: finished showSnackbar result=$res" }
        } catch (e: CancellationException) {
            TRACE(WARNING) { "NoticeHost: CANCELLED (host disposed or key changed)" }
        }
        TRACE(WARNING) { "NoticeHost: consume ${n.id}" }
        gameManager.consumeNotice(n)
    }

    SnackbarHost(hostState = hostState) { Snackbar(snackbarData = it) }
}


/*
@Composable
fun NoticeHost() { // 🟠 changed: English comment
    val gameManager = LocalGameManager.current
    val state by gameManager.gameState.collectAsState()
    val notices = state.pendingNotices

    val hostState = remember { SnackbarHostState() }

    DisposableEffect(Unit) {
        TRACE(TraceLogLevel.WARNING) { "NoticeHost: mounted" }
        onDispose { TRACE(TraceLogLevel.WARNING) { "NoticeHost: disposed" } }
    }

    // When the head of the queue changes, display it
    LaunchedEffect(notices.firstOrNull()?.id) {
        val n = notices.firstOrNull() ?: return@LaunchedEffect
        val duration = when (n.kind) {
            NoticeKind.Info, NoticeKind.Success -> SnackbarDuration.Short
            NoticeKind.Warning -> SnackbarDuration.Short
            NoticeKind.Error -> SnackbarDuration.Long
        }
        TRACE (TraceLogLevel.WARNING) { "NoticeHost: showing notice id=${n.id} kind=${n.kind} msg='${n.message}'" }

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
*/