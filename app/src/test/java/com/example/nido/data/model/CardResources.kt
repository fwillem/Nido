package com.example.nido.data.model

import com.example.nido.R

// Object storing all image resources at compile time
object CardResources {
    val images = mapOf(
        // PINK (p)
        "p_1" to R.drawable.nido_card_p_1,
        "p_2" to R.drawable.nido_card_p_2,
        "p_3" to R.drawable.nido_card_p_3,
        "p_4" to R.drawable.nido_card_p_4,
        "p_5" to R.drawable.nido_card_p_5,
        "p_6" to R.drawable.nido_card_p_6,
        "p_7" to R.drawable.nido_card_p_7,
        "p_8" to R.drawable.nido_card_p_8,
        "p_9" to R.drawable.nido_card_p_9,
        // ORANGE (o)
        "o_1" to R.drawable.nido_card_o_1,
        "o_2" to R.drawable.nido_card_o_2,
        "o_3" to R.drawable.nido_card_o_3,
        "o_4" to R.drawable.nido_card_o_4,
        "o_5" to R.drawable.nido_card_o_5,
        "o_6" to R.drawable.nido_card_o_6,
        "o_7" to R.drawable.nido_card_o_7,
        "o_8" to R.drawable.nido_card_o_8,
        "o_9" to R.drawable.nido_card_o_9,
        // BLUE (b)
        "b_1" to R.drawable.nido_card_b_1,
        "b_2" to R.drawable.nido_card_b_2,
        "b_3" to R.drawable.nido_card_b_3,
        "b_4" to R.drawable.nido_card_b_4,
        "b_5" to R.drawable.nido_card_b_5,
        "b_6" to R.drawable.nido_card_b_6,
        "b_7" to R.drawable.nido_card_b_7,
        "b_8" to R.drawable.nido_card_b_8,
        "b_9" to R.drawable.nido_card_b_9,
        // RED (r)
        "r_1" to R.drawable.nido_card_r_1,
        "r_2" to R.drawable.nido_card_r_2,
        "r_3" to R.drawable.nido_card_r_3,
        "r_4" to R.drawable.nido_card_r_4,
        "r_5" to R.drawable.nido_card_r_5,
        "r_6" to R.drawable.nido_card_r_6,
        "r_7" to R.drawable.nido_card_r_7,
        "r_8" to R.drawable.nido_card_r_8,
        "r_9" to R.drawable.nido_card_r_9,
        // GREEN (g)
        "g_1" to R.drawable.nido_card_g_1,
        "g_2" to R.drawable.nido_card_g_2,
        "g_3" to R.drawable.nido_card_g_3,
        "g_4" to R.drawable.nido_card_g_4,
        "g_5" to R.drawable.nido_card_g_5,
        "g_6" to R.drawable.nido_card_g_6,
        "g_7" to R.drawable.nido_card_g_7,
        "g_8" to R.drawable.nido_card_g_8,
        "g_9" to R.drawable.nido_card_g_9,
        // MOCHA (m)
        "m_1" to R.drawable.nido_card_m_1,
        "m_2" to R.drawable.nido_card_m_2,
        "m_3" to R.drawable.nido_card_m_3,
        "m_4" to R.drawable.nido_card_m_4,
        "m_5" to R.drawable.nido_card_m_5,
        "m_6" to R.drawable.nido_card_m_6,
        "m_7" to R.drawable.nido_card_m_7,
        "m_8" to R.drawable.nido_card_m_8,
        "m_9" to R.drawable.nido_card_m_9
    )

    val backCoverCard = Card(
        cardImageId = R.drawable.back_cover,
        color = CardColor.MOCHA,
        value = 0
    )

    fun getImage(color: CardColor, value: Int): Int {
        return images["${color.letter}_$value"]
            ?: throw IllegalStateException("Missing resource for card: ${color.letter}_$value")
    }
}