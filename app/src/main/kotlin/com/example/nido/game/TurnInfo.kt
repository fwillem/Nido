package com.example.nido.game


data class TurnInfo(
    val canSkip: Boolean = false,                // false = player must play, he cannot skip
    val canGoAllIn: Boolean = false,            // Use can play all its card if he does have a valid combination, whatever the number opf cards currently on the mat
    val displaySkip: Boolean = false,           // tells if skip button should be displayed
    val displayPlay: Boolean = false,           // tells if play button should be displayed
    val displaySkipCounter: Boolean = false,    // tells if the skip button with counter should be displayed
    val displayRemove: Boolean = false,          // tells if the Remove button needs to be displayed
    val displayNotifyRemotePlayer: Boolean = false, // tells if the remote player button notifyRemotePlayer: Boolean = false,    // tells if the remote player button

    // DEBUG ONLY
    val debugDisplayAIsHands: Boolean = false,  // tells if the AI's hands should be displayed (debug mode value)
    val displayManualAIPlay: Boolean = false, // Show "Play AI" button (manual mode)
)

