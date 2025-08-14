package com.example.nido.data.model

import androidx.annotation.StringRes
import com.example.nido.R

enum class PlayerType(@StringRes val displayNameRes: Int) {
    LOCAL(R.string.human),
    AI(R.string.bot),
    REMOTE(R.string.remote_human)
}