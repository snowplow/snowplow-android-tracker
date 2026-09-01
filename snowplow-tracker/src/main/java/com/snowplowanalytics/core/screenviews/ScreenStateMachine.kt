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
import com.snowplowanalytics.snowplow.event.EndScreenView
import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.event.ScreenView
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent

class ScreenStateMachine : StateMachineInterface {
    /*
     States: Init, Screen
     Events: SV (ScreenView), ESV (EndScreenView)
     Transitions:
      - Init (SV) Screen
      - Screen (SV) Screen
      - Screen (ESV) Screen, marked as ended
     Entity Generation:
      - Screen, unless ended
     */

    override val identifier: String
        get() = ID

    override val subscribedEventSchemasForTransitions: List<String>
        get() = listOf(TrackerConstants.SCHEMA_SCREEN_VIEW, TrackerConstants.SCHEMA_END_SCREEN_VIEW)

    override val subscribedEventSchemasForEntitiesGeneration: List<String>
        get() = listOf("*")

    override val subscribedEventSchemasForPayloadUpdating: List<String>
        get() = listOf(TrackerConstants.SCHEMA_SCREEN_VIEW)

    override val subscribedEventSchemasForAfterTrackCallback: List<String>
        get() = emptyList()

    override val subscribedEventSchemasForFiltering: List<String>
        get() = emptyList()

    override val subscribedEventSchemasForEventsBefore: List<String>
        get() = emptyList()

    override fun transition(event: Event, state: State?): State? {
        return when (event) {
            is ScreenView -> ScreenState(
                id = event.id,
                name = event.name,
                type = event.type,
                transitionType = event.transitionType,
                fragmentClassName = event.fragmentClassName,
                fragmentTag = event.fragmentTag,
                activityClassName = event.activityClassName,
                activityTag = event.activityTag,
                previousScreenState = state as? ScreenState
            )
            is EndScreenView -> {
                val screenState = state as? ScreenState
                if (screenState != null && (event.screenId == null || event.screenId.toString() == screenState.id)) {
                    screenState.ended = true
                }
                state
            }
            else -> state
        }
    }

    override fun entities(event: InspectableEvent, state: State?): List<SelfDescribingJson>? {
        val screenState = state as? ScreenState ?: return ArrayList()
        // the EndScreenView event itself still reports the context of the screen it just ended;
        // only events strictly after it see the context cleared
        if (screenState.ended && event.schema != TrackerConstants.SCHEMA_END_SCREEN_VIEW) return ArrayList()
        return listOf(screenState.getCurrentScreen(true))
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
        return null
    }

    override fun eventsBefore(event: Event): List<Event>? {
        return null
    }

    companion object {
        val ID: String
            get() = "ScreenContext"
    }
}
