/*
 * Copyright (c) 2015-2020 Snowplow Analytics Ltd. All rights reserved.
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
import java.util.List;
import java.util.Map;

import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;

public class EventStoreTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Clean SQLite database
        DefaultEventStore eventStore = new DefaultEventStore(getContext(), 250);
        waitUntilDatabaseOpen(eventStore);
        eventStore.removeAllEvents();
        eventStore.close();
    }

    public void testAddEventOnEmptyStore() throws InterruptedException {
        DefaultEventStore eventStore = new DefaultEventStore(getContext(), 250);

        eventStore.add(getEvent());
        assertFalse(eventStore.isDatabaseOpen());
        assertEquals(0, eventStore.getEmittableEvents().size());

        waitUntilDatabaseOpen(eventStore);

        assertTrue(eventStore.isDatabaseOpen());
        assertEquals(1, eventStore.getEmittableEvents().size());
    }

    public void testAddEventOnNotEmptyStore() throws InterruptedException {
        DefaultEventStore eventStore = new DefaultEventStore(getContext(), 250);

        // fill eventStore with 1 event
        eventStore.add(getEvent());
        waitUntilDatabaseOpen(eventStore);
        assertEquals(1, eventStore.getEmittableEvents().size());
        eventStore.close();

        // add new event
        eventStore = new DefaultEventStore(getContext(), 250);
        eventStore.add(getEvent());
        assertFalse(eventStore.isDatabaseOpen());
        assertEquals(0, eventStore.getEmittableEvents().size());

        waitUntilDatabaseOpen(eventStore);

        assertTrue(eventStore.isDatabaseOpen());
        assertEquals(2, eventStore.getEmittableEvents().size());
    }

    public void testRemoveEventsOnNotEmptyStore() throws InterruptedException {
        DefaultEventStore eventStore = new DefaultEventStore(getContext(), 250);

        // fill eventStore with 1 event
        eventStore.add(getEvent());
        waitUntilDatabaseOpen(eventStore);
        assertEquals(1, eventStore.getEmittableEvents().size());
        eventStore.close();

        // add new event and remove when database closed
        eventStore = new DefaultEventStore(getContext(), 250);
        eventStore.add(getEvent());
        assertEquals(1, eventStore.getSize());
        eventStore.removeAllEvents();
        assertEquals(0, eventStore.getSize());
        assertFalse(eventStore.isDatabaseOpen());

        waitUntilDatabaseOpen(eventStore);

        assertTrue(eventStore.isDatabaseOpen());
        assertEquals(1, eventStore.getEmittableEvents().size());
    }

    // Tests with database already open

    public void testGetNonExistentEvent() throws InterruptedException {
        DefaultEventStore eventStore = getEventStore();

        Map<String, Object> event = eventStore.getEvent(-1);
        assertNull(event);

        eventStore.close();
        event = eventStore.getEvent(-1);
        assertNull(event);
    }

    public void testInsertPayload() throws InterruptedException {
        DefaultEventStore eventStore = getEventStore();

        long id = eventStore.insertEvent(getEvent());
        long lastRowId = eventStore.getLastInsertedRowId();
        Map<String, Object> event = eventStore.getEvent(id);

        assertEquals(id, lastRowId);
        assertEquals(1, eventStore.getSize());
        assertNotNull(event);

        eventStore.close();
        eventStore.insertEvent(getEvent());
    }

    public void testRemoveAllEvents() throws InterruptedException {
        DefaultEventStore eventStore = getEventStore();

        eventStore.add(getEvent());
        eventStore.add(getEvent());
        eventStore.add(getEvent());
        eventStore.add(getEvent());
        eventStore.add(getEvent());
        eventStore.add(getEvent());

        assertEquals(6, eventStore.getSize());
        eventStore.removeAllEvents();
        assertEquals(0, eventStore.getSize());
    }

    public void testRemoveIndividualEvent() throws InterruptedException {
        DefaultEventStore eventStore = getEventStore();

        long id = eventStore.insertEvent(getEvent());
        boolean res = eventStore.removeEvent(id);

        assertEquals(0, eventStore.getSize());
        assertTrue(res);

        res = eventStore.removeEvent(id);
        assertFalse(res);

        eventStore.close();
        res = eventStore.removeEvent(id);
        assertFalse(res);
    }

    public void testRemoveRangeOfEvents() throws InterruptedException {
        DefaultEventStore eventStore = getEventStore();

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

    public void testCloseDatabase() throws InterruptedException {
        DefaultEventStore eventStore = getEventStore();

        assertTrue(eventStore.isDatabaseOpen());
        eventStore.close();
        assertFalse(eventStore.isDatabaseOpen());
    }

    public void testUpgrade() {
        EventStoreHelper helper = EventStoreHelper.getInstance(getContext());
        SQLiteDatabase database = helper.getWritableDatabase();
        helper.onUpgrade(database, 1, 2);
    }

    // Helper Methods

    private DefaultEventStore getEventStore() throws InterruptedException {
        DefaultEventStore eventStore = new DefaultEventStore(getContext(), 250);
        waitUntilDatabaseOpen(eventStore);
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

    private void waitUntilDatabaseOpen(DefaultEventStore eventStore) throws InterruptedException {
        while (!eventStore.isDatabaseOpen()) {
            Thread.sleep(300);
        }
    }
}
