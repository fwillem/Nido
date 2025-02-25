package com.example.nido.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

// 🎨 Custom Game Colors
object NidoColors {
    val BackgroundDark = Color(0xFF1B1B1B)
    val BackgroundLight = Color(0xFFF5F5F5)
    val PrimaryText = Color.White
    val SecondaryText = Color(0xFF333333)
    val HighlightWinner = Color.Yellow
    val HighlightLoser = Color.Red
    val CardDefault = Color.DarkGray

    // 🎨 **New Colors for Mat & Discard Pile**
    val PlaymatBackground = Color(0xFF228B22)  // Forest Green (Used in MatView)
    val DiscardPileBackground = Color.Gray     // Default discard pile color
}

// 🎨 Dark Mode Colors
private val DarkColorScheme = darkColorScheme(
    background = NidoColors.BackgroundDark,
    surface = NidoColors.BackgroundDark,
    onBackground = NidoColors.PrimaryText,
    onSurface = NidoColors.PrimaryText
)

// 🎨 Light Mode Colors
private val LightColorScheme = lightColorScheme(
    background = NidoColors.BackgroundLight,
    surface = NidoColors.BackgroundLight,
    onBackground = NidoColors.SecondaryText,
    onSurface = NidoColors.SecondaryText
)

// 🌙 Handles Light/Dark Themes
@Composable
fun NidoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}