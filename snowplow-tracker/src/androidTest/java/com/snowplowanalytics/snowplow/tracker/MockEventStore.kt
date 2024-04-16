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
package com.snowplowanalytics.snowplow.tracker

import com.snowplowanalytics.core.tracker.Logger.v
import com.snowplowanalytics.snowplow.emitter.EventStore
import com.snowplowanalytics.snowplow.payload.Payload
import com.snowplowanalytics.snowplow.emitter.EmitterEvent
import com.snowplowanalytics.snowplow.payload.TrackerPayload
import java.util.ArrayList
import java.util.HashMap
import kotlin.time.Duration

class MockEventStore : EventStore {
    var db = HashMap<Long, Payload?>()
    var lastInsertedRow: Long = -1
    override fun add(payload: Payload) {
        synchronized(this) {
            lastInsertedRow++
            v("MockEventStore", "Add %s", payload)
            db.put(lastInsertedRow, payload)
        }
    }

    override fun removeEvent(id: Long): Boolean {
        synchronized(this) {
            v("MockEventStore", "Remove %s", id)
            return db.remove(id) != null
        }
    }

    override fun removeEvents(ids: MutableList<Long>): Boolean {
        var result = true
        for (id in ids) {
            val removed = removeEvent(id)
            result = result && removed
        }
        return result
    }

    override fun removeAllEvents(): Boolean {
        synchronized(this) {
            v("MockEventStore", "Remove all")
            db = HashMap()
            lastInsertedRow = 0
        }
        return true
    }

    override fun size(): Long {
        return db.size.toLong()
    }

    override fun getEmittableEvents(queryLimit: Int): List<EmitterEvent> {
        synchronized(this) {
            val eventIds: MutableList<Long> = ArrayList()
            val eventPayloads: MutableList<String> = ArrayList()
            var events: MutableList<EmitterEvent> = ArrayList()
            for ((key, value) in db) {
                val payloadCopy: Payload = TrackerPayload()
                payloadCopy.addMap(value!!.map)
                val event = EmitterEvent(payloadCopy, key)
                eventIds.add(event.eventId)
                eventPayloads.add(payloadCopy.map.toString())
                events.add(event)
            }
            if (queryLimit < events.size) {
                events = events.subList(0, queryLimit)
            }
            v("MockEventStore", "getEmittableEvents ids: %s", eventIds)
            v("MockEventStore", "getEmittableEvents payloads: %s", eventPayloads)
            return events
        }
    }

    override fun removeOldEvents(maxSize: Long, maxAge: Duration) {
        // "Not implemented in the mock event store"
    }
}
