package com.example.nido.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nido.R
import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType
import com.example.nido.game.LocalPlayer
import com.example.nido.game.ai.AIPlayer
import com.example.nido.game.multiplayer.RemotePlayer
import com.example.nido.ui.components.*
import com.example.nido.ui.preview.NidoPreview
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.utils.Constants
import com.example.nido.utils.Constants.GAME_DEFAULT_POINT_LIMIT
import com.example.nido.utils.Constants.GAME_MIN_PLAYERS
import com.example.nido.utils.Debug
import com.example.nido.utils.LanguagePicker
import java.util.UUID

// Style texte par défaut (hors noms de joueurs)
val DEFAULT_TEXT_STYLE: TextStyle
    @Composable get() = MaterialTheme.typography.titleMedium


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
            label = { Text(stringResource(R.string.your_name), style = DEFAULT_TEXT_STYLE) },
            trailingIcon = {
                IconButton(onClick = {
                    val trimmed = internalName.text.trim().ifBlank { "Jil" }
                    onNameChange(trimmed)
                    editing = false
                }) {
                    Icon(Icons.Default.Check, contentDescription = stringResource(R.string.done))
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
                contentDescription = stringResource(R.string.edit),
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 6.dp)
            )
        }
    }
}


@Composable
fun SetupScreen(
    initialPlayers: List<Player>,
    initialPointLimit: Int,
    debug: Debug,
    onDone: (List<Player>, Int, Debug) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aiPlayers = listOf(
        "Thorstein" to "⚡", "Erik" to "🪓", "Bjorn" to "🐻",
        "Lagertha" to "🛡", "Freydis" to "🔥", "Astrid" to "🌙"
    )

    var selectedPlayers by rememberSaveable { mutableStateOf(initialPlayers) }
    var selectedPointLimit by rememberSaveable { mutableStateOf(initialPointLimit) }
    var displayAIsHands by rememberSaveable { mutableStateOf(debug.displayAIsHands) }
    var aiDontAutoPlay by rememberSaveable { mutableStateOf(debug.doNotAutoPlayerAI) }
    var selectedLanguage by rememberSaveable { mutableStateOf(debug.language) }

    // 🔄 Animations preset + durée (ms) synchronisée
    var aiTimerPreset by rememberSaveable { mutableStateOf(AITimerPreset.fromDuration(debug.aiTimerDuration)) }
    var aiTimerDuration by rememberSaveable { mutableStateOf(aiTimerPreset.durationMs) }

    val context = LocalContext.current

    val stepSizePointLimit = 5
    val validStepsPointLimit = (Constants.GAME_MIN_POINT_LIMIT..Constants.GAME_MAX_POINT_LIMIT step stepSizePointLimit).toList()

    NidoScreenScaffold(
        cardInnerPaddingVertical = 8.dp,
        maxContentWidth = 700.dp,
        maxContentHeight = null
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            // Centered headline
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.game_setup),
                    style = DEFAULT_TEXT_STYLE,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Players line (inchangé)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.players, selectedPlayers.size), style = DEFAULT_TEXT_STYLE)
                EditablePlayerName(
                    name = selectedPlayers[0].name,
                    onNameChange = { newName ->
                        selectedPlayers = selectedPlayers.toMutableList().also {
                            it[0] = it[0].copy(name = newName)
                        }
                    }
                )
                selectedPlayers.drop(1).forEach { player ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${player.avatar} ${player.name}", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3 action buttons (inchangé)
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
                            selectedPlayers += AIPlayer(
                                id = UUID.randomUUID().toString(),
                                name = nextAI.first,
                                avatar = nextAI.second
                            )
                        }
                    },
                    enabled = selectedPlayers.size < Constants.GAME_MAX_PLAYERS
                ) { Text(stringResource(R.string.add_AI_player)) }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (selectedPlayers.size < Constants.GAME_MAX_PLAYERS) {
                            val remoteId = UUID.randomUUID().toString()
                            val name = context.getString(R.string.remote_player)
                            selectedPlayers += RemotePlayer(id = remoteId, name = name, avatar = "🌍")
                        }
                    },
                    enabled = selectedPlayers.none { it.playerType == PlayerType.REMOTE } &&
                            selectedPlayers.size < Constants.GAME_MAX_PLAYERS
                ) { Text(stringResource(R.string.add_remote_player)) }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { if (selectedPlayers.size > 1) selectedPlayers = selectedPlayers.dropLast(1) },
                    enabled = selectedPlayers.size > (GAME_MIN_PLAYERS - 1)
                ) { Text(stringResource(R.string.remove_player)) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Point Limit + Animations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Point Limit
                Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.point_limit),
                        style = DEFAULT_TEXT_STYLE,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(16.dp))
                    NumberStepperPill(
                        value = selectedPointLimit,
                        values = validStepsPointLimit,
                        onValueChange = { selectedPointLimit = it },
                        modifier = Modifier
                            .widthIn(min = 56.dp, max = 120.dp)
                            .height(40.dp),
                        wrapAround = false
                    ) { v ->
                        Text(
                            text = v.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1
                        )
                    }
                }

                // Animations (3 boutons)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.animations),
                        style = DEFAULT_TEXT_STYLE,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(16.dp))
                    AITimerSelector(
                        selected = aiTimerPreset,
                        onSelected = { preset ->
                            aiTimerPreset = preset
                            aiTimerDuration = preset.durationMs // garde la valeur ms alignée
                        },
                        modifier = Modifier.fillMaxWidth(),
                        height = 40.dp,
                       // width = 400,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Debug options (inchangé sauf style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguagePicker(
                    selectedLanguage = selectedLanguage,
                    onSelected = { selectedLanguage = it },
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Checkbox(checked = displayAIsHands, onCheckedChange = { displayAIsHands = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.display_ai_hands), style = DEFAULT_TEXT_STYLE)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Checkbox(checked = aiDontAutoPlay, onCheckedChange = { aiDontAutoPlay = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.disable_ai_autoplay), style = DEFAULT_TEXT_STYLE)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = {
                        val newDebug = debug.copy(
                            displayAIsHands = displayAIsHands,
                            doNotAutoPlayerAI = aiDontAutoPlay,
                            language = selectedLanguage,
                            aiTimerDuration = aiTimerDuration // ms
                        )
                        onDone(selectedPlayers, selectedPointLimit, newDebug)
                    },
                    enabled = selectedPlayers.size >= 2
                ) { Text(stringResource(R.string.done)) }
            }
        }
        Row(modifier = Modifier.align(Alignment.BottomEnd)) {
            Spacer(modifier = Modifier.weight(1f))
            VersionLabel(modifier = Modifier.align(Alignment.CenterVertically), option = VersionOptions.TAG)
        }
    }
}

// Preview intacte
@NidoPreview(name = "SetupScreen")
@Composable
fun SetupScreenPreview() {
    NidoTheme {
        SetupScreen(
            initialPlayers = listOf(
                LocalPlayer(id = "0", name = "Jil", avatar = "👤"),
                AIPlayer(id = "1", name = "Thorstein", avatar = "⚡"),
                AIPlayer(id = "2", name = "Erik", avatar = "🪓"),
                AIPlayer(id = "3", name = "Bjorn", avatar = "🐻"),
            ),
            initialPointLimit = GAME_DEFAULT_POINT_LIMIT,
            debug = Debug(displayAIsHands = true, doNotAutoPlayerAI = false),
            onDone = { _, _, _ -> },
            onCancel = {}
        )
    }
}
