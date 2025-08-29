

import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nido.ui.LocalGameManager

@Composable
fun WaitingRoomScreen(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gm = LocalGameManager.current
    val state by gm.gameState.collectAsState()
    val ms = state.multiplayerState
    val localReady  = ms?.localReady  ?: false
    val remoteReady = ms?.remoteReady ?: false

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Multiplayer", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.padding(12.dp))
        CircularProgressIndicator()
        Spacer(Modifier.padding(12.dp))
        Text(
            text = when {
                ms == null || ms.currentGameId == null -> "Connecting…"
                !localReady && !remoteReady            -> "Press the button on both devices"
                localReady && !remoteReady             -> "Waiting for the other player…"
                !localReady && remoteReady             -> "Other player is ready. Press the button!"
                else                                   -> "Starting game…"
            }
        )
        Spacer(Modifier.padding(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onCancel) { Text("Cancel") }
        }
    }
}
