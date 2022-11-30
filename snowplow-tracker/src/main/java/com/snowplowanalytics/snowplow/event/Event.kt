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

import com.snowplowanalytics.core.tracker.Tracker
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * The event interface
 */
interface Event {
    /**
     * @return the event custom contexts
     */
    val contexts: List<SelfDescribingJson>

    /**
     * @return the optional true events timestamp
     */
    val trueTimestamp: Long?

    /**
     * @return the event data payload
     */
    val dataPayload: Map<String, Any?>

    /**
     * Hook method called just before the event processing in order to execute special operations.
     * @apiNote Internal use only - Don't use in production, it can change without notice.
     */
    fun beginProcessing(tracker: Tracker)

    /**
     * Hook method called just after the event processing in order to execute special operations.
     * @apiNote Internal use only - Don't use in production, it can change without notice.
     */
    fun endProcessing(tracker: Tracker)
}
