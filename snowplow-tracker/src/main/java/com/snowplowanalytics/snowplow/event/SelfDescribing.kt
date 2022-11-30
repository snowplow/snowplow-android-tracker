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
package com.snowplowanalytics.snowplow.event

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.utils.Preconditions
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * A SelfDescribing event.
 */
class SelfDescribing : AbstractSelfDescribing {
    /**
     * The properties of the event. Has two field:
     * * a "data" field containing the event properties,
     * * a "schema" field identifying the schema against which the data is validated.
     */
    @JvmField
    val eventData: SelfDescribingJson
    
    // Tracker methods
    
    /** A "data" field containing the event properties.  */
    override val dataPayload: Map<String, Any?>

    /** A "schema" field identifying the schema against which the data is validated.  */
    override val schema: String

    /**
     * Creates a SelfDescribing event from a SelfDescribingJson.
     * @param eventData The properties of the event. Has two field: a "data" field containing the event
     * properties and a "schema" field identifying the schema against which the data is validated.
     */
    constructor(eventData: SelfDescribingJson) {
        val eventDataMap = eventData.map
        val payload = eventDataMap[Parameters.DATA] as Map<String, Any?>
        dataPayload = payload 
        
        val schema = eventDataMap[Parameters.SCHEMA] as String
        this.schema = schema
        
        this.eventData = eventData
    }

    /**
     * Creates a SelfDescribing event.
     * @param schema The schema against which the payload is validated.
     * @param payload The event properties.
     */
    constructor(schema: String, payload: Map<String, Any?>) {
        this.schema = schema
        dataPayload = payload
        eventData = SelfDescribingJson(schema, payload)
    }
}
