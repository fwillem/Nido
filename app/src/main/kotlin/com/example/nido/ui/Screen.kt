package com.example.nido.ui


sealed class AppScreen(val route: String) {
    object Splash : AppScreen(Routes.SPLASH)
    object Landing : AppScreen(Routes.LANDING)
    object Setup : AppScreen(Routes.SETUP)
    object Game : AppScreen(Routes.GAME)
    object Score : AppScreen(Routes.SCORE)

    // Object to hold route constants
    object Routes {
        const val SPLASH = "splash_screen"
        const val LANDING = "landing_screen"
        const val SETUP = "setup_screen"
        const val GAME = "game_screen"
        const val SCORE = "score_screen"
        const val MULTI_WAIT = "multi_wait"
    }
}