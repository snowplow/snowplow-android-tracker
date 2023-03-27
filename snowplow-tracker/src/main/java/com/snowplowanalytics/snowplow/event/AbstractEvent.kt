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
package com.snowplowanalytics.snowplow.event

import com.snowplowanalytics.core.tracker.Tracker
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import java.util.*

/**
 * Base AbstractEvent class which contains common elements to all events:
 * - Custom Context: list of custom contexts or null
 * - Timestamp: user defined event timestamp or 0
 * - AbstractEvent Id: a unique id for the event
 */
abstract class AbstractEvent : Event {
    /** List of custom entities associated with the event.  */
    /** List of custom contexts associated with the event.  */
    @Deprecated("Please use `entities`")
    @JvmField
    val customContexts: MutableList<SelfDescribingJson> = LinkedList()
    
    /**
     * @return the optional "true" (custom) event timestamp
     */
    override var trueTimestamp: Long? = null
    
    // Builder methods

    /** Adds a list of context entities to the existing ones. */
    fun entities(entities: List<SelfDescribingJson>?): AbstractEvent {
        entities?.let { customContexts.addAll(entities) }
        return this
    }

    /** Adds a list of context entities to the existing ones. */
    @Deprecated("Please use `entities()`")
    fun contexts(contexts: List<SelfDescribingJson>?): AbstractEvent {
        return entities(contexts)
    }

    /** Set the custom timestamp of the event.  */
    fun trueTimestamp(trueTimestamp: Long?): AbstractEvent {
        this.trueTimestamp = trueTimestamp
        return this
    }
    
    // Public methods
    
    /**
     * @return the event custom context entities
     */
    override val entities: MutableList<SelfDescribingJson>
        get() = customContexts

    @Deprecated("Please use `entities`")
    override val contexts: List<SelfDescribingJson>
        get() = entities

    override fun beginProcessing(tracker: Tracker) {}
    override fun endProcessing(tracker: Tracker) {}
}
