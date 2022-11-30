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
package com.snowplowanalytics.snowplow.network

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.snowplow.payload.Payload
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.payload.TrackerPayload

/**
 * Request class that contains the payloads to send
 * to the collector.
 */
class Request {
    @JvmField
    val payload: Payload
    @JvmField
    val emitterEventIds: List<Long>
    @JvmField
    val oversize: Boolean
    @JvmField
    val customUserAgent: String?
    
    /**
     * Create a request object.
     * @param payload to send to the collector.
     * @param id as reference of the event to send.
     * @param oversize indicates if the payload exceeded the maximum size allowed.
     */
    @JvmOverloads
    constructor(payload: Payload, id: Long, oversize: Boolean = false) {
        val ids: MutableList<Long> = ArrayList()
        ids.add(id)
        emitterEventIds = ids
        this.payload = payload
        this.oversize = oversize
        customUserAgent = getUserAgent(payload)
    }

    /**
     * Create a request object.
     * @param payloads to send to the collector as a payload bundle.
     * @param emitterEventIds as reference of the events to send.
     */
    constructor(payloads: List<Payload>, emitterEventIds: List<Long>) {
        var tempUserAgent: String? = null
        val payloadData = ArrayList<Map<*, *>>()
        for (payload in payloads) {
            payloadData.add(payload.map)
            tempUserAgent = getUserAgent(payload)
        }
        payload = TrackerPayload()
        val payloadBundle = SelfDescribingJson(TrackerConstants.SCHEMA_PAYLOAD_DATA, payloadData)
        payload.addMap(payloadBundle.map as Map<String, Any>)
        this.emitterEventIds = emitterEventIds
        customUserAgent = tempUserAgent
        oversize = false
    }

    /**
     * Get the User-Agent string for the request's header.
     *
     * @param payload The payload where to get the `ua` parameter.
     * @return User-Agent string from subject settings or the default one.
     */
    private fun getUserAgent(payload: Payload): String? {
        val hashMap = payload.map as HashMap<*, *>
        return hashMap[Parameters.USERAGENT] as String?
    }
}
