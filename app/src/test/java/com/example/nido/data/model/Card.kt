package com.example.nido.data.model

import androidx.annotation.DrawableRes
import com.example.nido.HAND_SIZE


data class Card(
    @DrawableRes val cardImageId: Int,
    val color: CardColor,
    val isPartOfCompositions: Array<Boolean> = Array(HAND_SIZE) { false },
    val value: Int
)
