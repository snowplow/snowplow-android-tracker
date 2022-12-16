package com.snowplowanalytics.core.tracker

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.event.ScreenView
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent

class ScreenStateMachine : StateMachineInterface {
    /*
     States: Init, Screen
     Events: SV (ScreenView)
     Transitions:
      - Init (SV) Screen
      - Screen (SV) Screen
     Entity Generation:
      - Screen
     */
    
    override fun subscribedEventSchemasForTransitions(): List<String> {
        return listOf(TrackerConstants.SCHEMA_SCREEN_VIEW)
    }

    override fun subscribedEventSchemasForEntitiesGeneration(): List<String> {
        return listOf("*")
    }

    override fun subscribedEventSchemasForPayloadUpdating(): List<String> {
        return listOf(TrackerConstants.SCHEMA_SCREEN_VIEW)
    }

    override fun transition(event: Event, state: State?): State? {
        val screenView = event as ScreenView
        val screenState: ScreenState = if (state != null) {
            // - Screen (SV) Screen
            state as ScreenState
        } else {
            // - Init (SV) Screen
            ScreenState()
        }
        screenState.updateScreenState(
            screenView.id,
            screenView.name,
            screenView.type,
            screenView.transitionType,
            screenView.fragmentClassName,
            screenView.fragmentTag,
            screenView.activityClassName,
            screenView.activityTag
        )
        return screenState
    }

    override fun entities(event: InspectableEvent, state: State?): List<SelfDescribingJson> {
        if (state == null) return ArrayList()
        val screenState = state as ScreenState
        val entity = screenState.getCurrentScreen(true)
        return listOf(entity)
    }

    override fun payloadValues(event: InspectableEvent, state: State?): Map<String, Any>? {
        if (state is ScreenState) {
            val addedValues: MutableMap<String, Any> = HashMap()
            var value = state.previousName
            if (value != null && value.isNotEmpty()) {
                addedValues[Parameters.SV_PREVIOUS_NAME] = value
            }
            value = state.previousId
            if (value != null && value.isNotEmpty()) {
                addedValues[Parameters.SV_PREVIOUS_ID] = value
            }
            value = state.previousType
            if (value != null && value.isNotEmpty()) {
                addedValues[Parameters.SV_PREVIOUS_TYPE] = value
            }
            return addedValues
        }
        return null
    }
}
