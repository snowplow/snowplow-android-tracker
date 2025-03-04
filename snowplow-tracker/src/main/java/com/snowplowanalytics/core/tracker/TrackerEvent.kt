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
package com.snowplowanalytics.core.tracker

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.event.WebViewReader
import com.snowplowanalytics.core.statemachine.StateMachineEvent
import com.snowplowanalytics.core.statemachine.TrackerState
import com.snowplowanalytics.core.statemachine.TrackerStateSnapshot
import com.snowplowanalytics.snowplow.event.AbstractPrimitive
import com.snowplowanalytics.snowplow.event.AbstractSelfDescribing
import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.event.TrackerError
import com.snowplowanalytics.snowplow.payload.Payload
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import java.util.*

class TrackerEvent @JvmOverloads constructor(event: Event, state: TrackerStateSnapshot? = null) :
    StateMachineEvent {
    
    override var schema: String? = null
    override var name: String? = null
    override lateinit var payload: MutableMap<String, Any>
    override lateinit var state: TrackerStateSnapshot
    override lateinit var entities: MutableList<SelfDescribingJson>

    var eventId: UUID = UUID.randomUUID()
    var timestamp: Long = System.currentTimeMillis()
    var trueTimestamp: Long?
    var isPrimitive = false
    var isService: Boolean
    var isWebView = false

    init {
        entities = event.entities.toMutableList()
        trueTimestamp = event.trueTimestamp
        // NOTE: this code is a workaround since the types of the `Event.dataPayload` and `TrackerEvent.payload` don't match
        // `Event` allows the values to be optional, while `TrackerEvent` does not.
        // We should fix this in the next major version to unify the types.
        // The `toMap()` is called in order to make a copy before calling HashMap as that can lead to concurrency issues, see #692
        payload = HashMap(event.dataPayload.toMap())

        if (state != null) {
            this.state = state
        } else {
            this.state = TrackerState()
        }
        
        isService = event is TrackerError
        when (event) {
            is WebViewReader -> {
                name = payload[Parameters.EVENT]?.toString()
                schema = getWebViewSchema()
                isWebView = true
            }
            is AbstractPrimitive -> {
                name = event.name
                isPrimitive = true
            }
            else -> {
                schema = (event as? AbstractSelfDescribing)?.schema
                isPrimitive = false
            }
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

    fun addContextEntity(entity: SelfDescribingJson) {
        entities.add(entity)
    }

    fun wrapEntitiesToPayload(payload: Payload, base64Encoded: Boolean) {
        if (entities.isEmpty()) {
            return
        }

        val data: MutableList<Map<String, Any?>> = LinkedList()
        for (entity in entities) {
            data.add(entity.map)
        }
        val finalContext = SelfDescribingJson(TrackerConstants.SCHEMA_CONTEXTS, data)
        payload.addMap(
            finalContext.map,
            base64Encoded,
            Parameters.CONTEXT_ENCODED,
            Parameters.CONTEXT
        )
    }

    fun wrapPropertiesToPayload(toPayload: Payload, base64Encoded: Boolean) {
        when {
            isWebView -> wrapWebViewToPayload(toPayload, base64Encoded)
            isPrimitive -> toPayload.addMap(payload)
            else -> wrapSelfDescribingEventToPayload(toPayload, base64Encoded)
        }
    }
    
    private fun getWebViewSchema(): String? {
        val selfDescribingData = payload[Parameters.WEBVIEW_EVENT_DATA] as SelfDescribingJson?
        return selfDescribingData?.map?.get(Parameters.SCHEMA)?.toString()
    }

    private fun addSelfDescribingDataToPayload(toPayload: Payload, base64Encoded: Boolean, data: SelfDescribingJson) {
        val unstructuredEventPayload = HashMap<String?, Any?>()
        unstructuredEventPayload[Parameters.SCHEMA] = TrackerConstants.SCHEMA_UNSTRUCT_EVENT
        unstructuredEventPayload[Parameters.DATA] = data.map
        toPayload.addMap(
            unstructuredEventPayload,
            base64Encoded,
            Parameters.UNSTRUCTURED_ENCODED,
            Parameters.UNSTRUCTURED
        )
    }
    
    private fun wrapWebViewToPayload(toPayload: Payload, base64Encoded: Boolean) {
        val selfDescribingData = payload[Parameters.WEBVIEW_EVENT_DATA] as SelfDescribingJson?
        if (selfDescribingData != null) {
            addSelfDescribingDataToPayload(toPayload, base64Encoded, selfDescribingData)
        }
        toPayload.addMap(payload.filterNot { it.key == Parameters.WEBVIEW_EVENT_DATA })
    }

    private fun wrapSelfDescribingEventToPayload(toPayload: Payload, base64Encoded: Boolean) {
        val schema = schema ?: return
        addSelfDescribingDataToPayload(toPayload, base64Encoded, SelfDescribingJson(schema, payload))
    }
}
