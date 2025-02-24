package com.example.nido.multiplayer

import com.example.nido.data.model.Player
import com.example.nido.data.model.PlayerType

class RemotePlayer(id: String, name: String) : Player(id, name, 0, PlayerType.REMOTE)
