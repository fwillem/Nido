package com.example.nido.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.example.nido.ui.dialogs.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.nido.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.nido.ui.components.VersionLabel
import androidx.compose.ui.tooling.preview.Preview
import com.example.nido.game.LocalPlayer
import com.example.nido.game.ai.AIPlayer
import com.example.nido.ui.preview.NidoPreview
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.utils.Constants.GAME_DEFAULT_POINT_LIMIT
import com.example.nido.utils.Constants.SPLASH_SCREEN_TIMEOUT

@Composable
fun SplashScreen(onExit: () -> Unit, modifier: Modifier = Modifier) {
    // Kick off a 2 second timer
    LaunchedEffect(Unit) {
        delay(SPLASH_SCREEN_TIMEOUT)
        onExit()
    }

    // Full-screen image
    Box(
        modifier = Modifier
            .clickable { onExit() }
            .fillMaxSize()
            .background(Color.White) // or whatever your bg is
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.53f),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Tap to start...",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(modifier = Modifier.align(Alignment.BottomEnd)) {
            Spacer(modifier = Modifier.weight(1f))
            VersionLabel(modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}


// @Preview(showBackground = true)
@NidoPreview(name = "SetupScreen")
@Composable
fun SplashScreenPreview() {
    NidoTheme {
        SplashScreen(onExit = {})
    }
}

