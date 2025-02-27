// GameViewModel.kt
package com.example.nido.game

import androidx.lifecycle.ViewModel
import com.example.nido.game.GameManager

class GameViewModel : ViewModel() {
    val gameManager = GameManager() // Create an instance of GameManager
}