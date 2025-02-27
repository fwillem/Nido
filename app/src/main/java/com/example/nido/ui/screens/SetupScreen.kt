package com.example.nido.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nido.data.model.Player
import com.example.nido.utils.Constants
import com.example.nido.game.LocalPlayer
import com.example.nido.game.ai.AIPlayer
import com.example.nido.ui.theme.NidoTheme



@Composable
fun SetupScreen(onGameStart: (List<Player>, Int) -> Unit, modifier: Modifier = Modifier) {
    // ✅ Viking AI Players (Names + Emojis)
    val aiPlayers = listOf(
        "Thorstein" to "⚡",  // God of Thunder vibes ⚡
        "Erik" to "🪓",       // Erik the Red, famous Viking explorer 🪓
        "Bjorn" to "🐻",      // Bjorn Ironside (means "Bear") 🐻
        "Lagertha" to "🛡",   // Shieldmaiden, strong female warrior 🛡
        "Freydis" to "🔥",    // Fearless explorer, fire spirit 🔥
        "Astrid" to "🌙"      // Mystical and wise 🌙
    )

    // ✅ Default player: YOU (local human player)
    var selectedPlayers by remember { mutableStateOf<List<Player>>(listOf(LocalPlayer(id = "0",name = "You",avatar = "👤"))) }

    // ✅ Game Point Limit (Slider)
    var selectedPointLimit by remember { mutableStateOf(Constants.GAME_DEFAULT_POINT_LIMIT) }
    val stepSize = 5
    val validSteps = (Constants.GAME_MIN_POINT_LIMIT..Constants.GAME_MAX_POINT_LIMIT step stepSize).toList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Game Setup", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ Display current selected players
        selectedPlayers.forEach { player ->
            Text("${player.avatar} ${player.name}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ Button to add AI players
        Button(
            onClick = {
                if (selectedPlayers.size < Constants.GAME_MAX_PLAYERS) {  // Max 6 players (1 Human + 5 AI)
                    val nextAI = aiPlayers[selectedPlayers.size - 1] // Pick next AI from list
                    selectedPlayers = selectedPlayers + AIPlayer(
                        id = (selectedPlayers.size).toString(),
                        name = nextAI.first,
                        avatar = nextAI.second
                    )
                }
            },
            enabled = selectedPlayers.size < Constants.GAME_MAX_PLAYERS  // Disable button if max players reached
        ) {
            Text("Add AI Player")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ Display number of selected players
        Text("Current Number of players: ${selectedPlayers.size}")

        // ✅ Slider to select point limit
        Text("Select Point Limit: $selectedPointLimit")

        Slider(
            value = selectedPointLimit.toFloat(),
            onValueChange = { newValue ->
                selectedPointLimit = validSteps.minByOrNull { kotlin.math.abs(it - newValue.toInt()) }
                    ?: Constants.GAME_DEFAULT_POINT_LIMIT
            },
            valueRange = Constants.GAME_MIN_POINT_LIMIT.toFloat()..Constants.GAME_MAX_POINT_LIMIT.toFloat(),
            steps = (validSteps.size - 2) // Ensure correct steps
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ✅ Start Game Button
        Button(
            onClick = { onGameStart(selectedPlayers, selectedPointLimit) },
            enabled = selectedPlayers.size >= 2  // Ensure at least 2 players
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
