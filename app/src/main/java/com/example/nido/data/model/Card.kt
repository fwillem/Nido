package com.example.nido.data.model

import androidx.annotation.DrawableRes
import com.example.nido.game.rules.HAND_SIZE
import com.example.nido.data.model.CardColor
import com.example.nido.data.model.CardResources



data class Card(
    @DrawableRes val cardImageId: Int,
    val color: CardColor,
    val isPartOfCompositions: Array<Boolean> = Array(HAND_SIZE) { false },
    val value: Int
) {
    // Secondary constructor for simpler test creation.
    constructor(value: Int, color: String) : this(
        cardImageId = CardResources.getImage(CardColor.valueOf(color.uppercase()), value),
        color = CardColor.valueOf(color.uppercase()),
        value = value
    )
}
