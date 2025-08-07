package com.example.nido.game.engine


import com.example.nido.game.GameEvent
import com.example.nido.game.GameSideEffect
import com.example.nido.game.GameState
import com.example.nido.game.ReducerResult
import java.util.concurrent.atomic.AtomicBoolean

class GameEventDispatcher(
    private val getState: () -> GameState,
    private val updateState: (GameState) -> Unit,
    private val handleSideEffect: (GameSideEffect) -> Unit,
    private val reducer: (GameState, GameEvent) -> ReducerResult
) {
    private val isEnqueuing = AtomicBoolean(false)
    private val eventQueue = mutableListOf<GameEvent>()
    private var isProcessingEvents = false



    fun enqueueEvent(event: GameEvent) {
            eventQueue.add(event)
            processEventQueue()
    }


    private fun processEventQueue() {
        if (isProcessingEvents) {
            // If already processing, just return to avoid re-entrancy
            return
        }
        isProcessingEvents = true
        try {
            // Process events in a loop to handle follow-up events
            while (eventQueue.isNotEmpty()) {
                val currentEvent = eventQueue.removeAt(0)
                val state = getState()
                val result = reducer(state, currentEvent)

                updateState(result.newState)

                result.followUpEvents.forEach { eventQueue.add(it) }
                result.sideEffects.forEach { handleSideEffect(it) }
            }
        } finally {
            isProcessingEvents = false
        }
    }





}
