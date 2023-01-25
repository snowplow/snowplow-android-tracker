package com.snowplowanalytics.core.tracker

import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent

interface StateMachineInterface {
    fun subscribedEventSchemasForTransitions(): List<String>
    fun subscribedEventSchemasForEntitiesGeneration(): List<String>
    fun subscribedEventSchemasForPayloadUpdating(): List<String>
    fun transition(event: Event, state: State?): State?
    fun entities(event: InspectableEvent, state: State?): List<SelfDescribingJson>?
    fun payloadValues(event: InspectableEvent, state: State?): Map<String, Any>?
}
