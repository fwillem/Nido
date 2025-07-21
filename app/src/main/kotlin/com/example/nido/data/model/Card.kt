package com.example.nido.data.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.nido.utils.Constants.HAND_SIZE


data class Card(
    @DrawableRes val cardImageId: Int,
    val color: CardColor,
    val isPartOfCompositions: Array<Boolean> = Array(HAND_SIZE) { false },
    val value: Int
) {
    // Local UI-only state — not used for logic, sync, or serialization
    var isSelected by mutableStateOf(false)

    // Secondary constructor for testing
    constructor(value: Int, color: String) : this(
        cardImageId = CardResources.getImage(CardColor.valueOf(color.uppercase()), value),
        color = CardColor.valueOf(color.uppercase()),
        value = value
    )

    override fun toString(): String {
        return "$value/$color"
    }
}
