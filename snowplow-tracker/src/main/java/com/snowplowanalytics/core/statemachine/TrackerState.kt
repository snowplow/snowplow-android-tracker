package com.snowplowanalytics.core.statemachine

class TrackerState : TrackerStateSnapshot {
    private var trackerState = HashMap<String, StateFuture>()
    
    @Synchronized
    fun put(stateIdentifier: String, state: StateFuture) {
        trackerState[stateIdentifier] = state
    }

    @Synchronized
    fun getStateFuture(stateIdentifier: String): StateFuture? {
        return trackerState[stateIdentifier]
    }

    fun removeState(stateIdentifier: String) {
        trackerState.remove(stateIdentifier)
    }

    @get:Synchronized
    val snapshot: TrackerStateSnapshot
        get() {
            val newTrackerState = TrackerState()
            newTrackerState.trackerState = HashMap(trackerState)
            return newTrackerState
        }

    // Implements TrackerStateSnapshot
    override fun getState(stateIdentifier: String): State? {
        val stateFuture = getStateFuture(stateIdentifier) ?: return null
        return stateFuture.state()
    }
}
