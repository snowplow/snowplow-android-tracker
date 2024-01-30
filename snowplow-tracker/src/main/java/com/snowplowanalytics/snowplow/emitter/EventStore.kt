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
package com.snowplowanalytics.snowplow.emitter

import com.snowplowanalytics.snowplow.payload.Payload
import kotlin.time.Duration

/**
 * The component that persists and buffers events before sending.
 */
interface EventStore {
    /**
     * Adds an event to the store.
     * @param payload the payload to be added
     */
    fun add(payload: Payload)

    /**
     * Removes an event from the store.
     * @param id the identifier of the event in the store.
     * @return a boolean of success to remove.
     */
    fun removeEvent(id: Long): Boolean

    /**
     * Removes a range of events from the store.
     * @param ids the events' identifiers in the store.
     * @return a boolean of success to remove.
     */
    fun removeEvents(ids: MutableList<Long>): Boolean

    /**
     * Empties the store of all the events.
     * @return a boolean of success to remove.
     */
    fun removeAllEvents(): Boolean

    /**
     * Returns amount of events currently in the store.
     * @return the count of events in the store.
     */
    fun size(): Long

    /**
     * Returns a list of [EmitterEvent] objects which contain events and related IDs.
     * @return EmitterEvent objects containing eventIds and event payloads.
     */
    fun getEmittableEvents(queryLimit: Int): List<EmitterEvent>

    /**
     * Remove events older than `maxAge` seconds and keep only the latest `maxSize` events.
     * @param maxSize the maximum number of events to keep.
     * @param maxAge the maximum age of events to keep.
     */
    fun removeOldEvents(maxSize: Long, maxAge: Duration)
}
