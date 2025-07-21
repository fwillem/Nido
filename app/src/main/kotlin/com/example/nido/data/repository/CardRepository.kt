package com.example.nido.data.repository

import androidx.compose.runtime.mutableStateListOf
import com.example.nido.data.model.Card
import com.example.nido.data.model.CardColor
import com.example.nido.data.model.CardResources



// ✅ Convert to `object` so we can access methods without instantiation
object CardRepository {


    // ✅ Get a specific card image
    fun getCardImage(color: CardColor, value: Int): Int {
        return CardResources.getImage(color, value)
    }

    // ✅ Get the back cover card
    fun getBackCover(): Card {
        return CardResources.backCoverCard
    }
}
