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

import com.snowplowanalytics.snowplow.configuration.PluginAfterTrackConfiguration
import com.snowplowanalytics.snowplow.configuration.PluginEntitiesConfiguration
import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent
import java.util.*

class PluginStateMachine(
    override val identifier: String,
    val entitiesConfiguration: PluginEntitiesConfiguration?,
    val afterTrackConfiguration: PluginAfterTrackConfiguration?
) : StateMachineInterface {

    override val subscribedEventSchemasForTransitions: List<String>
        get() = emptyList()

    override val subscribedEventSchemasForEntitiesGeneration: List<String>
        get() {
            val config = entitiesConfiguration ?: return emptyList()
            return config.schemas ?: Collections.singletonList("*")
        }

    override val subscribedEventSchemasForPayloadUpdating: List<String>
        get() = emptyList()

    override val subscribedEventSchemasForAfterTrackCallback: List<String>
        get() {
            val config = afterTrackConfiguration ?: return emptyList()
            return config.schemas ?: Collections.singletonList("*")
        }

    override fun transition(event: Event, state: State?): State? {
        return null
    }

    override fun entities(event: InspectableEvent, state: State?): List<SelfDescribingJson> {
        return entitiesConfiguration?.closure?.apply(event) ?: emptyList()
    }

    override fun payloadValues(event: InspectableEvent, state: State?): Map<String, Any>? {
        return null
    }

    override fun afterTrack(event: InspectableEvent) {
        afterTrackConfiguration?.closure?.accept(event)
    }
}
