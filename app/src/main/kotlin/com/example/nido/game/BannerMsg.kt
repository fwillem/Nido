package com.example.nido.game

sealed class BannerMsg {
    data class Play(val name: String) : BannerMsg()
}