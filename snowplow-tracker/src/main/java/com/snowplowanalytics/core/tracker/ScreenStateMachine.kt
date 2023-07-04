/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.core.tracker

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.statemachine.State
import com.snowplowanalytics.core.statemachine.StateMachineInterface
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

    override val identifier: String
        get() = ID
    
    override val subscribedEventSchemasForTransitions: List<String>
        get() = listOf(TrackerConstants.SCHEMA_SCREEN_VIEW)

    override val subscribedEventSchemasForEntitiesGeneration: List<String>
        get() = listOf("*")

    override val subscribedEventSchemasForPayloadUpdating: List<String>
        get() = listOf(TrackerConstants.SCHEMA_SCREEN_VIEW)

    override val subscribedEventSchemasForAfterTrackCallback: List<String>
        get() = emptyList()

    override val subscribedEventSchemasForFiltering: List<String>
        get() = emptyList()

    override fun transition(event: Event, state: State?): State? {
        val screenView = event as? ScreenView
        val screenState: ScreenState? = if (state != null) {
            // - Screen (SV) Screen
            state as? ScreenState
        } else {
            // - Init (SV) Screen
            ScreenState()
        }
        screenView?.let {
            screenState?.updateScreenState(
                it.id,
                it.name,
                it.type,
                it.transitionType,
                it.fragmentClassName,
                it.fragmentTag,
                it.activityClassName,
                it.activityTag
            )
        }
        return screenState
    }

    override fun entities(event: InspectableEvent, state: State?): List<SelfDescribingJson>? {
        if (state == null) return ArrayList()
        val screenState = state as? ScreenState
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
        return null
    }

    companion object {
        val ID: String
            get() = "ScreenContext"
    }
}
