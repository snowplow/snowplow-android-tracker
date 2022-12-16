package com.snowplowanalytics.core.tracker

import com.snowplowanalytics.snowplow.entity.LifecycleEntity
import com.snowplowanalytics.snowplow.event.Background
import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.event.Foreground
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent

class LifecycleStateMachine : StateMachineInterface {
    /*
     States: Visible, NotVisible
     Events: FG (Foreground), BG (Background)
     Transitions:
      - Visible (BG) NotVisible
      - NotVisible (FG) Visible
     Entity Generation:
      - Visible, NotVisible
     */
    
    override fun subscribedEventSchemasForTransitions(): List<String> {
        return listOf(Background.schema, Foreground.schema)
    }

    override fun subscribedEventSchemasForEntitiesGeneration(): List<String> {
        return listOf("*")
    }

    override fun subscribedEventSchemasForPayloadUpdating(): List<String> {
        return emptyList()
    }

    override fun transition(event: Event, currentState: State?): State? {
        if (event is Foreground) {
            return LifecycleState(true, event.foregroundIndex)
        }
        if (event is Background) {
            return LifecycleState(false, event.backgroundIndex)
        }
        return null
    }

    override fun entities(event: InspectableEvent, state: State?): List<SelfDescribingJson> {
        if (state == null) return listOf<SelfDescribingJson>(LifecycleEntity(true))
        
        val s = state as LifecycleState
        return listOf<SelfDescribingJson>(LifecycleEntity(s.isForeground).index(s.index))
    }

    override fun payloadValues(event: InspectableEvent, state: State?): Map<String, Any>? {
        return null
    }
}
