package com.example.nido.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
//import androidx.compose.ui.text.input.KeyboardActions
//import androidx.compose.ui.text.input.KeyboardOptions

import androidx.compose.foundation.text.KeyboardActions // Added import
import androidx.compose.foundation.text.KeyboardOptions // Added import

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.data.model.Player
import com.example.nido.game.LocalPlayer
import com.example.nido.game.ai.AIPlayer
import com.example.nido.ui.components.VersionLabel
import com.example.nido.ui.preview.NidoPreview
import com.example.nido.ui.theme.NidoColors
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.utils.Constants


@Composable
fun EditablePlayerName(
    name: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf(false) }
    var internalName by remember { mutableStateOf(TextFieldValue(name)) }

    if (editing) {
        OutlinedTextField(
            value = internalName,
            onValueChange = { internalName = it.copy(text = it.text.take(16)) },
            singleLine = true,
            modifier = modifier,
            label = { Text("Your Name") },
            trailingIcon = {
                IconButton(onClick = {
                    val trimmed = internalName.text.trim().ifBlank { "Jil" }
                    onNameChange(trimmed)
                    editing = false
                }) {
                    Icon(Icons.Default.Check, contentDescription = "Done")
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    val trimmed = internalName.text.trim().ifBlank { "Jil" }
                    onNameChange(trimmed)
                    editing = false
                }
            )
        )
    } else {
        Row(
            modifier = modifier
                .clickable {
                    editing = true
                    internalName = TextFieldValue(name)
                }
                .padding(vertical = 4.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("👤", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(name, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit",
                modifier = Modifier.size(16.dp).padding(start = 6.dp)
            )
        }
    }
}
@Composable
fun SetupScreen(onGameStart: (List<Player>, Int) -> Unit, modifier: Modifier = Modifier) {
    val aiPlayers = listOf(
        "Thorstein" to "⚡",
        "Erik" to "🪓",
        "Bjorn" to "🐻",
        "Lagertha" to "🛡",
        "Freydis" to "🔥",
        "Astrid" to "🌙"
    )

    var selectedPlayers by remember {
        mutableStateOf<List<Player>>(
            listOf(LocalPlayer(id = "0", name = "Jil", avatar = "👤"))
        )
    }

    var selectedPointLimit by remember { mutableStateOf(Constants.GAME_DEFAULT_POINT_LIMIT) }
    val stepSize = 5
    val validSteps = (Constants.GAME_MIN_POINT_LIMIT..Constants.GAME_MAX_POINT_LIMIT step stepSize).toList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NidoColors.ScoreScreenBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Game Setup", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Players (${selectedPlayers.size}) :")
                // Editable name for the first (local) player
                EditablePlayerName(
                    name = selectedPlayers[0].name,
                    onNameChange = { newName ->
                        selectedPlayers = selectedPlayers.toMutableList().also {
                            it[0] = it[0].copy(name = newName)
                        }
                    }
                )
                // The AI players (show as static text)
                selectedPlayers.drop(1).forEach { player ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${player.avatar} ${player.name}", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons (same width)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (selectedPlayers.size < Constants.GAME_MAX_PLAYERS) {
                            val nextAI = aiPlayers[selectedPlayers.size - 1]
                            selectedPlayers = selectedPlayers + AIPlayer(
                                id = (selectedPlayers.size).toString(),
                                name = nextAI.first,
                                avatar = nextAI.second
                            )
                        }
                    },
                    enabled = selectedPlayers.size < Constants.GAME_MAX_PLAYERS
                ) {
                    Text("Add Player")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (selectedPlayers.size > 1) {
                            selectedPlayers = selectedPlayers.dropLast(1)
                        }
                    },
                    enabled = selectedPlayers.size > 1
                ) {
                    Text("Remove Player")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Point Limit: $selectedPointLimit")
            Slider(
                value = selectedPointLimit.toFloat(),
                onValueChange = { newValue ->
                    selectedPointLimit =
                        validSteps.minByOrNull { kotlin.math.abs(it - newValue.toInt()) }
                            ?: Constants.GAME_DEFAULT_POINT_LIMIT
                },
                valueRange = Constants.GAME_MIN_POINT_LIMIT.toFloat()..Constants.GAME_MAX_POINT_LIMIT.toFloat(),
                steps = (validSteps.size - 2)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onGameStart(selectedPlayers, selectedPointLimit) },
                enabled = selectedPlayers.size >= 2
            ) {
                Text("Start Game")
            }

            VersionLabel()
        }
    }
}

// @Preview(showBackground = true)
@NidoPreview(name = "SetupScreeny")
@Composable
fun SetupScreenPreview() {
    NidoTheme {
        SetupScreen(onGameStart = { _, _ -> })
    }
}
