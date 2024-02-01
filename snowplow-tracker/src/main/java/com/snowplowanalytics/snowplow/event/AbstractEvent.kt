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
import java.util.*

/**
 * Base AbstractEvent class which contains common elements that can be added to all events:
 * - Entities: list of custom context entities
 * - "True" timestamp: user-defined custom event timestamp
 */
abstract class AbstractEvent : Event {
    private var _entities = mutableListOf<SelfDescribingJson>()
    /**
     * @return the custom context entities associated with the event.
     */
    override var entities: MutableList<SelfDescribingJson>
        get() {
            if (isProcessing) {
                entitiesForProcessing?.let {
                    return (_entities + it).toMutableList()
                }
            }
            return _entities
        }
        set(value) {
            _entities = value
        }

    @Deprecated("Old nomenclature", ReplaceWith("entities"))
    override var contexts: List<SelfDescribingJson>
        get() = entities
        set(value) { entities = value.toMutableList()}
    
    /**
     * @return the optional "true" (custom) event timestamp
     */
    override var trueTimestamp: Long? = null
    
    /** Used for events whose properties are added as entities, e.g. Ecommerce events */
    open val entitiesForProcessing: List<SelfDescribingJson>?
        get() = null
    
    // Builder methods

    /** Adds a list of context entities to the existing ones. */
    fun entities(entities: List<SelfDescribingJson>): AbstractEvent {
        this.entities.addAll(entities)
        return this
    }

    /** Adds a list of context entities to the existing ones. */
    @Deprecated("Old nomenclature.", ReplaceWith("entities()"))
    fun contexts(contexts: List<SelfDescribingJson>): AbstractEvent {
        return entities(contexts)
    }

    /** Set the custom timestamp of the event.  */
    fun trueTimestamp(trueTimestamp: Long?): AbstractEvent {
        this.trueTimestamp = trueTimestamp
        return this
    }
    

    private var isProcessing = false
    override fun beginProcessing(tracker: Tracker) {
        isProcessing = true
    }
    override fun endProcessing(tracker: Tracker) {
        isProcessing = false
    }
}
