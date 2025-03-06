package com.example.nido.data.model

import androidx.compose.ui.graphics.Color

enum class CardColor(val letter: Char, val uiColor: Color) {
    PINK('p', Color(0xFFE78BB9)),
    ORANGE('o', Color(0xFFF58A40)),
    BLUE('b', Color(0xFF2F78B3)),
    RED('r', Color(0xFF992D38)),
    GREEN('g', Color(0xFF5CBF82)),
    MOCHA('m', Color(0xFF573E36))
}
