/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.core.screenviews

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.statemachine.State
import com.snowplowanalytics.core.statemachine.StateMachineInterface
import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.event.ScreenEnd
import com.snowplowanalytics.snowplow.event.ScreenView
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent

class ScreenStateMachine : StateMachineInterface {
    /*
     States: Init, Screen, EndedScreen
     Events: SV (ScreenView), SE (ScreenEnd)
     Transitions:
      - Init (SV) Screen
      - Screen (SV) Screen
      - Screen (SE) EndedScreen
      - EndedScreen (SV) Screen
     Entity Generation:
      - Screen
      - EndedScreen (only for the SE that ended the screen)
     */

    /** Whether events tracked with their own screen entity keep it instead of getting the current screen entity. */
    @Volatile
    var screenContextFromEvent = false

    override val identifier: String
        get() = ID
    
    override val subscribedEventSchemasForTransitions: List<String>
        get() = listOf(TrackerConstants.SCHEMA_SCREEN_VIEW, TrackerConstants.SCHEMA_SCREEN_END)

    override val subscribedEventSchemasForEntitiesGeneration: List<String>
        get() = listOf("*")

    override val subscribedEventSchemasForPayloadUpdating: List<String>
        get() = listOf(TrackerConstants.SCHEMA_SCREEN_VIEW)

    override val subscribedEventSchemasForAfterTrackCallback: List<String>
        get() = emptyList()

    override val subscribedEventSchemasForFiltering: List<String>
        get() = listOf(TrackerConstants.SCHEMA_SCREEN_END)

    override val subscribedEventSchemasForEventsBefore: List<String>
        get() = emptyList()

    override fun transition(event: Event, state: State?): State? {
        if (event is ScreenEnd) {
            // keep the ended screen so that the next screen view can reference it as previous
            return (state as? ScreenState)?.ended()
        }
        return (event as? ScreenView)?.let { screenView ->
            ScreenState(
                id = screenView.id,
                name = screenView.name,
                type = screenView.type,
                transitionType = screenView.transitionType,
                fragmentClassName = screenView.fragmentClassName,
                fragmentTag = screenView.fragmentTag,
                activityClassName = screenView.activityClassName,
                activityTag = screenView.activityTag,
                previousScreenState = state as? ScreenState
            )
        }
    }

    override fun entities(event: InspectableEvent, state: State?): List<SelfDescribingJson>? {
        if (state == null) return ArrayList()
        // keep the screen entity attached by the caller rather than adding the current screen
        if (screenContextFromEvent && hasScreenEntity(event)) return null
        val screenState = state as? ScreenState
        when (screenState?.lifecycle) {
            // only the screen end event is attributed to the ended screen
            ScreenLifecycle.ENDING -> if (event.schema != TrackerConstants.SCHEMA_SCREEN_END) return null
            ScreenLifecycle.ENDED -> return null
            else -> {}
        }
        val entity = screenState?.getCurrentScreen(true)
        return entity?.let { listOf(it) }
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

    override fun afterTrack(event: InspectableEvent) {
    }

    override fun filter(event: InspectableEvent, state: State?): Boolean? {
        // only track the screen end event if it ended an active screen
        if (event.schema == TrackerConstants.SCHEMA_SCREEN_END) {
            return (state as? ScreenState)?.lifecycle == ScreenLifecycle.ENDING
        }
        return null
    }

    override fun eventsBefore(event: Event): List<Event>? {
        return null
    }

    /**
     * Whether the event was tracked with its own screen entity, of any 1-x-x version.
     * Screen view and screen end events always keep the screen they describe.
     */
    private fun hasScreenEntity(event: InspectableEvent): Boolean {
        if (event.schema == TrackerConstants.SCHEMA_SCREEN_VIEW || event.schema == TrackerConstants.SCHEMA_SCREEN_END) {
            return false
        }
        return event.entities.any { (it.map["schema"] as? String)?.startsWith(SCREEN_SCHEMA_PREFIX) == true }
    }

    companion object {
        val ID: String
            get() = "ScreenContext"

        private const val SCREEN_SCHEMA_PREFIX = "iglu:com.snowplowanalytics.mobile/screen/jsonschema/1-"
    }
}
