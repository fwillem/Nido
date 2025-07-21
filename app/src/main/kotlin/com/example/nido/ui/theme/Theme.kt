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

    val PlaymatBackground =     Color(0xFF228B22)  // Forest Green (Used in MatView)
    val PlayersRowBackground =  Color(0xFF004000) // Orange (Used in PlayersRowView) OK
    val MatViewBackground =     Color(0xFF228B22)   // Forest Green (Used in MatView)
    val HandViewBackground2 =   Color(0xFF006400)   // Blue (Used in HandView)
    val HandViewBackground =    Color(0xFF006400)  // Green (Used in HandView)
    val SelectedCardBackground =    Color(0xFF228B22)  // Green (Used in HandView)
    val PlayMatButtonBackground = Color(0xFF006400)
    val SetupScreenBackground = Color(0xFF228B22)
    val ScoreScreenBackground = Color(0xFF228B22)
    val ScoreScreenWinner = Color(0xFFAEEA00)
    val LandingMainStroke = Color(0xFF006400)
    val LandingButtonsBackground = Color(0xFF006400)
    val LandingMainBackground = Color(0xFF228B22)


    val DialogTitleBackground = Color(0xFF228B22)
/**
 *  DEBUG Colors
    val PlaymatBackground =    Color(0xFFE040FB)  // Green (Used in HandView)
    val PlayersRowBackground =  Color(0xFFF06400) // Orange (Used in PlayersRowView) OK
    val MatViewBackground =     Color(0xFF228B22)   // Forest Green (Used in MatView)
    val HandViewBackground2 =   Color(0xFF0000FF)   // Blue (Used in HandView)
    val HandViewBackground =    Color(0xFFFFFF00)  // Green (Used in HandView)
    val SelectedCardBackground =    Color(0xFF18FFFF)  // Green (Used in HandView)
*/


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