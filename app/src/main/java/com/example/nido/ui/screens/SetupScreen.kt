package com.example.nido.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.utils.Constants

@Composable
fun SetupScreen(onGameStart: (List<Player>, Int) -> Unit) {
    // ✅ Explicitly define the types to avoid "Not enough information to infer type argument"
    var selectedPlayers: List<Player> by remember { mutableStateOf(emptyList()) }
    var selectedPointLimit: Int by remember { mutableStateOf(Constants.GAME_DEFAULT_POINT_LIMIT) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Game Setup", style = MaterialTheme.typography.headlineSmall)

        // TODO: UI Components to Add Players (Local, AI, Remote)
        Button(
            onClick = {
                selectedPlayers = selectedPlayers + Player("1", "Alice", 0, PlayerType.LOCAL)
            }
        ) {
            Text("Add Local Player")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ Fixed point limit selection
        Text("Select Point Limit: $selectedPointLimit")
        Slider(
            value = selectedPointLimit.toFloat(),
            onValueChange = { selectedPointLimit = it.toInt() },
            valueRange = 10f..100f,
            steps = 9
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ✅ Start Game Button
        Button(
            onClick = { onGameStart(selectedPlayers, selectedPointLimit) },
            enabled = selectedPlayers.isNotEmpty()
        ) {
            Text("Start Game")
        }
    }
}
