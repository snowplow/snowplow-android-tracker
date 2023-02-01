package com.snowplowanalytics.core.statemachine

interface TrackerStateSnapshot {
    fun getState(stateIdentifier: String): State?
}
