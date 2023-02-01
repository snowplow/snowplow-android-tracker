package com.snowplowanalytics.core.statemachine

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
    
    override val subscribedEventSchemasForTransitions: List<String>
        get() = listOf(Background.schema, Foreground.schema)

    override val subscribedEventSchemasForEntitiesGeneration: List<String>
        get() = listOf("*")

    override val subscribedEventSchemasForPayloadUpdating: List<String>
        get() = emptyList()

    override fun transition(event: Event, currentState: State?): State? {
        if (event is Foreground) {
            return LifecycleState(true, event.foregroundIndex)
        }
        if (event is Background) {
            return LifecycleState(false, event.backgroundIndex)
        }
        return null
    }

    override fun entities(event: InspectableEvent, state: State?): List<SelfDescribingJson>? {
        if (state == null) return listOf<SelfDescribingJson>(LifecycleEntity(true))
        
        val s = state as? LifecycleState
        return s?.let { listOf<SelfDescribingJson>(LifecycleEntity(it.isForeground).index(it.index)) }
    }

    override fun payloadValues(event: InspectableEvent, state: State?): Map<String, Any>? {
        return null
    }
}
