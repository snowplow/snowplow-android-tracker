package com.snowplowanalytics.core.tracker

interface TrackerStateSnapshot {
    fun getState(stateIdentifier: String): State?
}
