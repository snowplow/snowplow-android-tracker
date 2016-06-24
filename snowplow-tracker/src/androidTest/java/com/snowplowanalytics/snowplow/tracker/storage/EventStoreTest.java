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

import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public void testGetNonExistentEvent() {
        EventStore eventStore = getEventStore();
        Map<String, Object> event = eventStore.getEvent(-1);
        assertNull(event);

        eventStore.close();
        event = eventStore.getEvent(-1);
        assertNull(event);
    }

    public void testInsertPayload() {
        EventStore eventStore = getEventStore();
        long id = eventStore.insertEvent(getEvent());
        long lastRowId = eventStore.getLastInsertedRowId();
        Map<String, Object> event = eventStore.getEvent(id);

        assertEquals(id, lastRowId);
        assertEquals(1, eventStore.getSize());
        assertNotNull(event);

        eventStore.close();
        eventStore.insertEvent(getEvent());
    }

    public void testEventStoreQueries() {
        EventStore eventStore = getEventStore();
        eventStore.insertEvent(getEvent());

        assertEquals(1, eventStore.getAllEvents().size());
        assertEquals(1, eventStore.getDescEventsInRange(1).size());
    }

    public void testRemoveAllEvents() {
        EventStore eventStore = getEventStore();

        eventStore.insertEvent(getEvent());
        eventStore.insertEvent(getEvent());
        eventStore.insertEvent(getEvent());
        eventStore.insertEvent(getEvent());
        eventStore.insertEvent(getEvent());
        eventStore.insertEvent(getEvent());

        assertEquals(6, eventStore.getSize());
        eventStore.removeAllEvents();
        assertEquals(0, eventStore.getSize());

        eventStore.close();
        boolean res = eventStore.removeAllEvents();
        assertFalse(res);
    }

    public void testRemoveIndividualEvent() {
        EventStore eventStore = getEventStore();
        long id = eventStore.insertEvent(getEvent());
        boolean res = eventStore.removeEvent(id);

        assertEquals(0, eventStore.getSize());
        assertEquals(true, res);

        res = eventStore.removeEvent(id);
        assertEquals(false, res);

        eventStore.close();
        res = eventStore.removeEvent(id);
        assertEquals(false, res);
    }

    public void testRemoveRangeOfEvents() {
        EventStore eventStore = getEventStore();
        List<Long> idList = new ArrayList<>();

        idList.add(eventStore.insertEvent(getEvent()));
        idList.add(eventStore.insertEvent(getEvent()));
        idList.add(eventStore.insertEvent(getEvent()));

        assertEquals(3, idList.size());
        assertEquals(3, eventStore.getSize());

        boolean res = eventStore.removeEvents(idList);

        assertEquals(0, eventStore.getSize());
        assertEquals(true, res);

        res = eventStore.removeEvents(idList);
        assertEquals(false, res);

        eventStore.close();
        res = eventStore.removeEvents(idList);
        assertEquals(false, res);

        res = eventStore.removeEvents(new ArrayList<Long>());
        assertEquals(false, res);
    }

    public void testCloseDatabase() {
        EventStore eventStore = getEventStore();
        assertEquals(true, eventStore.isDatabaseOpen());
        eventStore.open();
        assertEquals(true, eventStore.isDatabaseOpen());
        eventStore.close();
        assertEquals(false, eventStore.isDatabaseOpen());
    }

    public void testUpgrade() {
        EventStoreHelper helper = EventStoreHelper.getInstance(getContext());
        SQLiteDatabase database = helper.getWritableDatabase();
        helper.onUpgrade(database, 1, 2);
    }
}
