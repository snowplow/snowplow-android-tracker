package com.snowplowanalytics.snowplow.tracker

import com.snowplowanalytics.core.tracker.TrackerStateSnapshot

interface InspectableEvent {
    val schema: String?
    val name: String?
    val payload: Map<String?, Any?>
    val state: TrackerStateSnapshot
    
    fun addPayloadValues(payload: Map<String?, Any?>): Boolean
}
