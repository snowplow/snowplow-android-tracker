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
package com.snowplowanalytics.core.statemachine

import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent

interface StateMachineInterface {
    val identifier: String
    val subscribedEventSchemasForTransitions: List<String>
    val subscribedEventSchemasForEntitiesGeneration: List<String>
    val subscribedEventSchemasForPayloadUpdating: List<String>
    val subscribedEventSchemasForAfterTrackCallback: List<String>
    fun transition(event: Event, state: State?): State?
    fun entities(event: InspectableEvent, state: State?): List<SelfDescribingJson>?
    fun payloadValues(event: InspectableEvent, state: State?): Map<String, Any>?
    fun afterTrack(event: InspectableEvent)
}
