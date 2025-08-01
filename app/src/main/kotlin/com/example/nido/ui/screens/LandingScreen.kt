package com.example.nido.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nido.ui.components.NidoScreenScaffold
import com.example.nido.ui.preview.NidoPreview
import com.example.nido.ui.theme.NidoColors
import com.example.nido.ui.theme.NidoTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.nido.R

@Composable
fun LandingScreen(
    onSetup: () -> Unit,
    onGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonBoxSize = 160.dp
    val imageSize = (buttonBoxSize.value * 2f).dp  // 2x bigger than the box

    NidoScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Headline
            Text(
                text = stringResource(id = R.string.daring_to_challenge_the_gods),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .padding(bottom = 8.dp)
            )

            // Logo + Buttons horizontally aligned
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // Logo
                Image(
                    painter = painterResource(id = R.drawable.nido_logo),
                    contentDescription = "Nido Logo",
                    modifier = Modifier.size(imageSize)
                )

                // Button Box
                Box(
                    modifier = Modifier
                        .size(buttonBoxSize)
                        .background(
                            color = NidoColors.LandingButtonsBackground,
                            shape = RoundedCornerShape(32.dp)
                        )
                        .border(
                            width = 4.dp,
                            color = NidoColors.LandingMainStroke,
                            shape = RoundedCornerShape(32.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Button(
                            onClick = onGame,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(text = stringResource(R.string.play)) }
                        Button(
                            onClick = onSetup,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(text = stringResource(R.string.setup)) }
                    }
                }
            }
        }
    }
}

// @Preview(showBackground = true)
@NidoPreview(name = "LandingScreen")
@Composable
fun LandingScreenPreview() {
    NidoTheme {
        LandingScreen(
            onSetup = {},
            onGame = {},
        )
    }
}
