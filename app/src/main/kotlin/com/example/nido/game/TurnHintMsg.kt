package com.example.nido.game



sealed class TurnHintMsg {
    data class PlayerSkipped(val name: String) : TurnHintMsg()
    data class MatDiscardedNext(val name: String) : TurnHintMsg()
    object YouCannotBeat : TurnHintMsg()
    data class YouMustPlayOne(val canAllIn: Boolean) : TurnHintMsg()
    data class YouCanPlayNOrNPlusOne(val n: Int) : TurnHintMsg()
    data class YouKept(val card : String) : TurnHintMsg()
    data class PlayerKept(val name: String, val card : String) : TurnHintMsg()

    object Empty : TurnHintMsg() // fallback
}

