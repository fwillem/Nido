package com.example.nido.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nido.data.model.Player
import com.example.nido.game.players.LocalPlayer
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.utils.Constants

@Composable
fun SetupScreen(onGameStart: (List<Player>, Int) -> Unit) {
    // ✅ Explicitly define the types to avoid "Not enough information to infer type argument"
    var selectedPlayers by remember { mutableStateOf(emptyList<Player>()) }

    var selectedPointLimit by remember { mutableStateOf(Constants.GAME_DEFAULT_POINT_LIMIT) }

    val stepSize = 5  // Change this if needed
    val validSteps = (Constants.GAME_MIN_POINT_LIMIT..Constants.GAME_MAX_POINT_LIMIT step stepSize).toList()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Game Setup", style = MaterialTheme.typography.headlineSmall)

        // ✅ Button to add local player
        Button(
            onClick = { selectedPlayers = selectedPlayers + LocalPlayer("1", "Alice", 0) }
        ) {
            Text("Add Local Player")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ Display number of selected players
        Text("Current Number of players: ${selectedPlayers.size}")

        // ✅ Slider to select point limit
        Text("Select Point Limit: $selectedPointLimit")




        Slider(
            value = selectedPointLimit.toFloat(),
            onValueChange = { newValue ->
                selectedPointLimit = validSteps.minByOrNull { kotlin.math.abs(it - newValue.toInt()) } ?: Constants.GAME_DEFAULT_POINT_LIMIT
            },
            valueRange = Constants.GAME_MIN_POINT_LIMIT.toFloat()..Constants.GAME_MAX_POINT_LIMIT.toFloat(),
            steps = (validSteps.size - 2) // Subtract 2 to make sure steps match
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

/**
 * 🎨 **Preview for SetupScreen**
 * ✅ Simulates the screen without running the full app.
 */
@Preview(showBackground = true)
@Composable
fun SetupScreenPreview() {
    NidoTheme {
        SetupScreen(onGameStart = { _, _ -> })
    }
}
