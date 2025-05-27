package com.example.nido

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.nido.game.GameViewModel
import com.example.nido.ui.theme.NidoTheme
import com.example.nido.ui.screens.NidoApp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nido.game.IGameManager
import com.example.nido.game.getGameManagerInstance
import com.example.nido.ui.LocalGameManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.nido.utils.TRACE
import com.example.nido.utils.TraceLogLevel.*

import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
        val branch = getString(R.string.branch_name)
        val buildTime = getString(R.string.build_time)

        val bundle = Bundle().apply {

            /// putString("NIDO ${version}", "xxx")
            putString("Main_Activity", "xxx")
        }


         */
        Firebase.analytics.logEvent("nido_test", null)




        enableEdgeToEdge()

        // Enable immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())


        setContent {
            NidoTheme {
                val viewModel: GameViewModel = viewModel()

                // Obtain the GameManager instance using our internal helper.
                val gameManager: IGameManager = getGameManagerInstance()
                gameManager.initialize(viewModel)

                // Provide the GameManager only via LocalGameManager.
                CompositionLocalProvider(LocalGameManager provides gameManager) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NidoApp(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
