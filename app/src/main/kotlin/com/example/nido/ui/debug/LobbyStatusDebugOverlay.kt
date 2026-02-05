package com.example.nido.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.game.multiplayer.ConnectionStatus
import com.example.nido.game.multiplayer.LobbyState
import kotlinx.coroutines.flow.StateFlow

@Composable
fun LobbyStatusDebugOverlay(
    lobbyStateFlow: StateFlow<LobbyState>,
    modifier: Modifier = Modifier
) {
    val state by lobbyStateFlow.collectAsState()

    // ALLOW ALWAYS VISIBLE FOR DEBUGGING:
    // if (state.status == ConnectionStatus.Idle && state.error == null) return

    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(4.dp)
    ) {
        Text("LOBBY: ${state.status} | ROLE: ${state.role}", color = Color.White, fontSize = 10.sp)
        
        state.gameId?.let {
            Text("GameID: $it", color = Color.Yellow, fontSize = 10.sp)
        }
        
        if (state.myUid.isNotEmpty()) {
             Text("MyUID: ...${state.myUid.takeLast(6)}", color = Color.Gray, fontSize = 10.sp)
        }
        
        state.remoteUid?.let {
             Text("Remote: ...${it.takeLast(6)}", color = Color.Cyan, fontSize = 10.sp)
        }
        
        if (state.status != ConnectionStatus.Idle) {
             Text("Ready: Local=${state.localReady} Remote=${state.remoteReady}", color = Color.White, fontSize = 10.sp)
        }
        
        state.error?.let {
            Text("ERROR: $it", color = Color.Red, fontSize = 10.sp)
        }
    }
}
