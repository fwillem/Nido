package com.example.nido.ui

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaverScope // Import SaverScope if needed


sealed class AppScreen {
    object Setup : AppScreen()
    object Game : AppScreen()
    object Score : AppScreen()
}

val AppScreenSaver: Saver<AppScreen, String> = Saver(
    save = { screen ->
        when (screen) {
            AppScreen.Setup -> "setup"
            AppScreen.Game  -> "game"
            AppScreen.Score -> "score"
        }
    },
    restore = { value ->
        when (value) {
            "setup" -> AppScreen.Setup
            "game"  -> AppScreen.Game
            "score" -> AppScreen.Score
            else -> throw IllegalArgumentException("Unknown screen type: $value")
        }
    }
)

// NEW: Saver specifically for MutableState<AppScreen>
val mutableAppScreenSaver = Saver<MutableState<AppScreen>, String>(
    save = { mutableState -> // 'this' is the SaverScope here
        // Use the provided SaverScope (this) to call save, passing the value and the saver to use
        val saved = this.save(value = mutableState.value, saver = AppScreenSaver)
        // Ensure the result is String? and provide default if null
        saved as? String ?: "setup"
    },
    restore = { savedValue -> // Restore lambda doesn't have/need SaverScope
        // The restore part was already correct: use AppScreenSaver's restore directly
        mutableStateOf(AppScreenSaver.restore(savedValue) ?: AppScreen.Setup)
    }
)