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
package com.snowplowanalytics.core.tracker

import com.snowplowanalytics.snowplow.event.AbstractPrimitive
import com.snowplowanalytics.snowplow.event.AbstractSelfDescribing
import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.event.TrackerError
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent
import java.util.*

class TrackerEvent @JvmOverloads constructor(event: Event, state: TrackerStateSnapshot? = null) :
    InspectableEvent {
    
    override var schema: String? = null
    override var name: String? = null
    override lateinit var payload: MutableMap<String, Any>
    override lateinit var state: TrackerStateSnapshot
    
    var eventId: UUID = UUID.randomUUID()
    var timestamp: Long = System.currentTimeMillis()
    var trueTimestamp: Long?
    var contexts: MutableList<SelfDescribingJson>
    var isPrimitive = false
    var isService: Boolean

    init {
        contexts = event.contexts.toMutableList()
        trueTimestamp = event.trueTimestamp
        payload = HashMap(event.dataPayload)
        
        if (state != null) {
            this.state = state
        } else {
            this.state = TrackerState()
        }
        
        isService = event is TrackerError
        if (event is AbstractPrimitive) {
            name = event.name
            isPrimitive = true
        } else {
            schema = (event as? AbstractSelfDescribing)?.schema
            isPrimitive = false
        }
    }

    override fun addPayloadValues(payload: Map<String, Any>): Boolean {
        var result = true
        for ((key, value) in payload) {
            if (this.payload[key] == null) {
                this.payload[key] = value
            } else {
                result = false
            }
        }
        return result
    }
}
