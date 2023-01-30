/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.core.statemachine

import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.snowplow.entity.DeepLink
import com.snowplowanalytics.snowplow.event.DeepLinkReceived
import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent
import kotlin.collections.ArrayList

class DeepLinkStateMachine : StateMachineInterface {
    /*
     States: Init, DeepLinkReceived, ReadyForOutput
     Events: DL (DeepLinkReceived), SV (ScreenView)
     Transitions:
      - Init (DL) DeepLinkReceived
      - DeepLinkReceived (SV) ReadyForOutput
      - ReadyForOutput (DL) DeepLinkReceived
      - ReadyForOutput (SV) Init
     Entity Generation:
      - ReadyForOutput
      */

    override val identifier: String
        get() = ID

    override val subscribedEventSchemasForTransitions: List<String>
        get() = listOf(DeepLinkReceived.schema, TrackerConstants.SCHEMA_SCREEN_VIEW)

    override val subscribedEventSchemasForEntitiesGeneration: List<String> 
        get() = listOf(TrackerConstants.SCHEMA_SCREEN_VIEW)

    override val subscribedEventSchemasForPayloadUpdating: List<String>
        get() = ArrayList()

    override val subscribedEventSchemasForAfterTrackCallback: List<String>
        get() = emptyList()

    override fun transition(event: Event, state: State?): State? {
        // - Init (DL) DeepLinkReceived
        // - ReadyForOutput (DL) DeepLinkReceived
        return if (event is DeepLinkReceived) {
            DeepLinkState(event.url, event.referrer)
        } else {
            // - Init (SV) Init
            if (state == null) {
                return null
            }
            // - ReadyForOutput (SV) Init
            val dlState = state as? DeepLinkState
            if (dlState?.readyForOutput == true) {
                return null
            }
            // - DeepLinkReceived (SV) ReadyForOutput
            val currentState = dlState?.let { DeepLinkState(it.url, it.referrer) }
            currentState?.readyForOutput = true
            currentState
        }
    }

    override fun entities(event: InspectableEvent, state: State?): List<SelfDescribingJson>? {
        if (state == null) { return null }

        val deepLinkState = state as? DeepLinkState
        if (deepLinkState?.readyForOutput == false) {
            return null
        }
        val entity = deepLinkState?.let { DeepLink(it.url).referrer(it.referrer) }
        return entity?.let { listOf<SelfDescribingJson>(entity) }
    }

    override fun payloadValues(event: InspectableEvent, state: State?): Map<String, Any>? {
        return null
    }

    override fun afterTrack(event: InspectableEvent) {
    }

    companion object {
        val ID: String
            get() = "DeepLinkContext"
    }
}
