/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker.storage;

import android.test.AndroidTestCase;

import java.util.Map;

import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;

public class EventStoreTest extends AndroidTestCase {

    // Helper Methods

    private EventStore getEventStore() {
        EventStore eventStore = new EventStore(getContext(), 250);
        eventStore.removeAllEvents();
        return eventStore;
    }

    private SelfDescribingJson getEvent() {
        TrackerPayload trackerPayload = new TrackerPayload();
        trackerPayload.add("someKey", "someValue");
        trackerPayload.add("anotherKey", "anotherValue");

        return new SelfDescribingJson(
                "iglu:com.snowplowanalytics.snowplow/example/jsonschema/1-0-0",
                trackerPayload);
    }

    // Tests

    public void testInsertPayload() {
        EventStore eventStore = getEventStore();
        long id = eventStore.insertEvent(getEvent());
        long lastRowId = eventStore.getLastInsertedRowId();
        Map<String, Object> event = eventStore.getEvent(id);

        assertEquals(id, lastRowId);
        assertEquals(1, eventStore.getSize());
        assertNotNull(event);
    }

    public void testEventStoreQueries() {
        EventStore eventStore = getEventStore();
        eventStore.insertEvent(getEvent());

        assertEquals(1, eventStore.getAllEvents().size());
        assertEquals(1, eventStore.getDescEventsInRange(1).size());
    }

    public void testRemoveAllEvents() {
        EventStore eventStore = getEventStore();

        // Add 6 events
        eventStore.insertEvent(getEvent());
        eventStore.insertEvent(getEvent());
        eventStore.insertEvent(getEvent());
        eventStore.insertEvent(getEvent());
        eventStore.insertEvent(getEvent());
        eventStore.insertEvent(getEvent());

        assertEquals(6, eventStore.getSize());

        eventStore.removeAllEvents();

        assertEquals(0, eventStore.getSize());
    }

    public void testRemoveIndividualEvent() {
        EventStore eventStore = getEventStore();
        long id = eventStore.insertEvent(getEvent());
        boolean res = eventStore.removeEvent(id);

        assertEquals(0, eventStore.getSize());
        assertEquals(true, res);
    }

    public void testCloseDatabase() {
        EventStore eventStore = getEventStore();

        assertEquals(true, eventStore.isDatabaseOpen());

        eventStore.close();

        assertEquals(false, eventStore.isDatabaseOpen());
    }
}
