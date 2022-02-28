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

package com.snowplowanalytics.snowplow.internal.emitter.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.snowplowanalytics.snowplow.emitter.EmitterEvent;
import com.snowplowanalytics.snowplow.payload.Payload;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.payload.TrackerPayload;

public class EventStoreTest extends AndroidTestCase {

    static int QUERY_LIMIT = 150;

    private static List<SQLiteEventStore> openedEventStores = new ArrayList<>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        for (SQLiteEventStore eventStore : openedEventStores) {
            eventStore.close();
        }
        openedEventStores.clear();
        EventStoreHelper.removeUnsentEventsExceptForNamespaces(getContext(), new ArrayList<>());
    }

    public void testAddEventOnEmptyStore() throws InterruptedException {
        SQLiteEventStore eventStore = new SQLiteEventStore(getContext(), "namespace");
        openedEventStores.add(eventStore);

        eventStore.add(getEvent());
        assertFalse(eventStore.isDatabaseOpen());
        assertEquals(0, eventStore.getEmittableEvents(QUERY_LIMIT).size());

        waitUntilDatabaseOpen(eventStore);

        assertTrue(eventStore.isDatabaseOpen());
        assertEquals(1, eventStore.getEmittableEvents(QUERY_LIMIT).size());
    }

    public void testAddEventOnNotEmptyStore() throws InterruptedException {
        SQLiteEventStore eventStore = new SQLiteEventStore(getContext(), "namespace");
        openedEventStores.add(eventStore);

        // fill eventStore with 1 event
        TrackerPayload trackerPayload = new TrackerPayload();
        trackerPayload.add("k", "v");
        eventStore.add(trackerPayload);
        waitUntilDatabaseOpen(eventStore);
        List<EmitterEvent> list = eventStore.getEmittableEvents(QUERY_LIMIT);
        assertEquals(1, eventStore.getEmittableEvents(QUERY_LIMIT).size());
        eventStore.close();

        // add new event
        eventStore = new SQLiteEventStore(getContext(), "namespace");
        openedEventStores.add(eventStore);
        eventStore.add(getEvent());
        assertFalse(eventStore.isDatabaseOpen());
        assertEquals(0, eventStore.getEmittableEvents(QUERY_LIMIT).size());

        waitUntilDatabaseOpen(eventStore);

        assertTrue(eventStore.isDatabaseOpen());
        assertEquals(2, eventStore.getEmittableEvents(QUERY_LIMIT).size());
    }

    public void testRemoveEventsOnNotEmptyStore() throws InterruptedException {
        SQLiteEventStore eventStore = new SQLiteEventStore(getContext(), "namespace");
        openedEventStores.add(eventStore);

        // fill eventStore with 1 event
        eventStore.add(getEvent());
        waitUntilDatabaseOpen(eventStore);
        assertEquals(1, eventStore.getEmittableEvents(QUERY_LIMIT).size());
        eventStore.close();

        // add new event and remove when database closed
        eventStore = new SQLiteEventStore(getContext(), "namespace");
        openedEventStores.add(eventStore);
        eventStore.add(getEvent());
        assertEquals(1, eventStore.getSize());
        eventStore.removeAllEvents();
        assertEquals(0, eventStore.getSize());
        assertFalse(eventStore.isDatabaseOpen());

        waitUntilDatabaseOpen(eventStore);

        assertTrue(eventStore.isDatabaseOpen());
        assertEquals(1, eventStore.getEmittableEvents(QUERY_LIMIT).size());
    }

    // Tests with database already open

    public void testGetNonExistentEvent() throws InterruptedException {
        SQLiteEventStore eventStore = getEventStore();

        Map<String, Object> event = eventStore.getEvent(-1);
        assertNull(event);

        eventStore.close();
        event = eventStore.getEvent(-1);
        assertNull(event);
    }

    public void testInsertPayload() throws InterruptedException {
        SQLiteEventStore eventStore = getEventStore();

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
        SQLiteEventStore eventStore = getEventStore();

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
        SQLiteEventStore eventStore = getEventStore();

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
        SQLiteEventStore eventStore = getEventStore();

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
        SQLiteEventStore eventStore = getEventStore();

        assertTrue(eventStore.isDatabaseOpen());
        eventStore.close();
        assertFalse(eventStore.isDatabaseOpen());
    }

    public void testUpgrade() {
        EventStoreHelper helper = EventStoreHelper.getInstance(getContext(), "namespace");
        SQLiteDatabase database = helper.getWritableDatabase();
        helper.onUpgrade(database, 1, 2);
        helper.close();
    }

    public void testEventStoreCreateDatabase() throws InterruptedException {
        Context context = getContext();
        SQLiteEventStore eventStore = new SQLiteEventStore(context, "namespace");
        openedEventStores.add(eventStore);
        waitUntilDatabaseOpen(eventStore);
        List<String> databaseList = Arrays.asList(context.databaseList());
        assertTrue(databaseList.contains("snowplowEvents-namespace.sqlite"));
        assertTrue(databaseList.contains("snowplowEvents-namespace.sqlite-wal"));
        assertTrue(databaseList.contains("snowplowEvents-namespace.sqlite-shm"));
    }

    public void testEventStoreRemoveDatabases() throws InterruptedException {
        Context context = getContext();
        SQLiteEventStore eventStore1 = new SQLiteEventStore(context, "namespace1");
        SQLiteEventStore eventStore2 = new SQLiteEventStore(context, "namespace2");
        SQLiteEventStore eventStore3 = new SQLiteEventStore(context, "namespace3");
        openedEventStores.addAll(Arrays.asList(eventStore1, eventStore2, eventStore3));
        waitUntilDatabaseOpen(eventStore1);
        waitUntilDatabaseOpen(eventStore2);
        waitUntilDatabaseOpen(eventStore3);
        // Remove database
        EventStoreHelper.removeUnsentEventsExceptForNamespaces(context, Arrays.asList("namespace2"));
        List<String> databaseList = Arrays.asList(context.databaseList());
        assertFalse(databaseList.contains("snowplowEvents-namespace1.sqlite"));
        assertFalse(databaseList.contains("snowplowEvents-namespace1.sqlite-wal"));
        assertFalse(databaseList.contains("snowplowEvents-namespace1.sqlite-shm"));
        assertTrue(databaseList.contains("snowplowEvents-namespace2.sqlite"));
        assertTrue(databaseList.contains("snowplowEvents-namespace2.sqlite-wal"));
        assertTrue(databaseList.contains("snowplowEvents-namespace2.sqlite-shm"));
        assertFalse(databaseList.contains("snowplowEvents-namespace3.sqlite"));
        assertFalse(databaseList.contains("snowplowEvents-namespace3.sqlite-wal"));
        assertFalse(databaseList.contains("snowplowEvents-namespace3.sqlite-shm"));
    }

    public void testEventStoreInvalidNamespaceConversion() throws InterruptedException {
        Context context = getContext();
        SQLiteEventStore eventStore = new SQLiteEventStore(context, "namespace*.^?1ò2@");
        openedEventStores.add(eventStore);
        waitUntilDatabaseOpen(eventStore);
        List<String> databaseList = Arrays.asList(context.databaseList());
        assertTrue(databaseList.contains("snowplowEvents-namespace-1-2-.sqlite"));
    }

    public void testMigrationFromLegacyToNamespacedEventStore() throws InterruptedException {
        Context context = getContext();

        // Create fake legacy database
        SQLiteOpenHelper legacyDB = new SQLiteOpenHelper(context, "snowplowEvents.sqlite", null, 1) {
            @Override
            public void onCreate(SQLiteDatabase sqLiteDatabase) {
                sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS 'events' (id INTEGER PRIMARY KEY)");
            }
            @Override
            public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) { }
        };
        SQLiteDatabase db = legacyDB.getWritableDatabase();
        db.enableWriteAheadLogging();
        legacyDB.close();
        List<String> databaseList = Arrays.asList(context.databaseList());
        // old DB
        assertTrue(databaseList.contains("snowplowEvents.sqlite"));
        // old DB is closed so these should be false
        assertFalse(databaseList.contains("snowplowEvents.sqlite-wal"));
        assertFalse(databaseList.contains("snowplowEvents.sqlite-shm"));
        // new DB not yet created so these should be false
        assertFalse(databaseList.contains("snowplowEvents-namespace.sqlite"));
        assertFalse(databaseList.contains("snowplowEvents-namespace.sqlite-wal"));
        assertFalse(databaseList.contains("snowplowEvents-namespace.sqlite-shm"));

        // Migrate database when SQLiteEventStore is launched the first time
        SQLiteEventStore eventStore = new SQLiteEventStore(context, "namespace");
        openedEventStores.add(eventStore);
        waitUntilDatabaseOpen(eventStore);
        databaseList = Arrays.asList(context.databaseList());
        assertFalse(databaseList.contains("snowplowEvents.sqlite"));
        assertFalse(databaseList.contains("snowplowEvents.sqlite-wal"));
        assertFalse(databaseList.contains("snowplowEvents.sqlite-shm"));
        assertTrue(databaseList.contains("snowplowEvents-namespace.sqlite"));
        assertTrue(databaseList.contains("snowplowEvents-namespace.sqlite-wal"));
        assertTrue(databaseList.contains("snowplowEvents-namespace.sqlite-shm"));
    }

    public void testMultipleAccessToSameSQLiteFile() throws InterruptedException {
        Context context = getContext();
        SQLiteEventStore eventStore1 = new SQLiteEventStore(context, "namespace");
        openedEventStores.add(eventStore1);
        waitUntilDatabaseOpen(eventStore1);
        TrackerPayload trackerPayload = new TrackerPayload();
        trackerPayload.add("key1", "value1");
        eventStore1.insertEvent(trackerPayload);
        assertEquals(1, eventStore1.getSize());

        SQLiteEventStore eventStore2 = new SQLiteEventStore(context, "namespace");
        openedEventStores.add(eventStore2);
        waitUntilDatabaseOpen(eventStore2);
        trackerPayload = new TrackerPayload();
        trackerPayload.add("key2", "value2");
        eventStore2.insertEvent(trackerPayload);
        assertEquals(2, eventStore2.getSize());
    }

    // Helper Methods

    private SQLiteEventStore getEventStore() throws InterruptedException {
        SQLiteEventStore eventStore = new SQLiteEventStore(getContext(), "namespace");
        openedEventStores.add(eventStore);
        waitUntilDatabaseOpen(eventStore);
        eventStore.removeAllEvents();
        return eventStore;
    }

    private Payload getEvent() {
        TrackerPayload trackerPayload = new TrackerPayload();
        trackerPayload.add("someKey", "someValue");
        trackerPayload.add("anotherKey", "anotherValue");
        SelfDescribingJson event = new SelfDescribingJson(
                "iglu:com.snowplowanalytics.snowplow/example/jsonschema/1-0-0",
                trackerPayload);
        TrackerPayload result = new TrackerPayload();
        result.addMap(event.getMap());
        return result;
    }

    private void waitUntilDatabaseOpen(SQLiteEventStore eventStore) throws InterruptedException {
        while (!eventStore.isDatabaseOpen()) {
            Thread.sleep(300);
        }
    }
}
