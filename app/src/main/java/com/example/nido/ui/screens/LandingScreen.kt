package com.example.nido.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A landing screen that gives users options to go to setup or start the game directly.
 *
 * @param onSetup callback when the "Setup" button is clicked
 * @param onGame callback when the "New Game" button is clicked
 * @param modifier optional modifier for styling and layout
 */
@Composable
fun LandingScreen(
    onSetup: () -> Unit,
    onGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Nido!",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Button(
            onClick = onSetup,
            modifier = Modifier
                .fillMaxSize(0.6f)
                .padding(bottom = 16.dp)
        ) {
            Text(text = "Setup Game")
        }

        Button(
            onClick = onGame,
            modifier = Modifier
                .fillMaxSize(0.6f)
        ) {
            Text(text = "New Game")
        }
    }
}
