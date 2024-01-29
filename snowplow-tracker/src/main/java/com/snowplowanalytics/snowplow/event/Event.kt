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
package com.snowplowanalytics.snowplow.event

import com.snowplowanalytics.core.tracker.Tracker
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * The Event interface
 */
interface Event {
    /**
     * The list of entities associated with an event.
     * @return the event custom context entities
     */
    val entities: MutableList<SelfDescribingJson>

    /**
     * @Deprecated 
     * @return the event custom context entities
     */
    @Deprecated("Please use `entities`")
    val contexts: List<SelfDescribingJson>

    /**
     * Set a custom timestamp.
     * @return the optional true events timestamp
     */
    val trueTimestamp: Long?

    /**
     * The payload for the event.
     * @return the event data payload
     */
    val dataPayload: Map<String, Any?>

    /**
     * Internal use only - Don't use in production, it can change without notice.
     * 
     * Hook method called just before the event processing in order to execute special operations.
     * @suppress
     */
    fun beginProcessing(tracker: Tracker)

    /**
     * Internal use only - Don't use in production, it can change without notice.
     * 
     * Hook method called just after the event processing in order to execute special operations.
     * @suppress
     */
    fun endProcessing(tracker: Tracker)
}
