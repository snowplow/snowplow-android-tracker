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
package com.snowplowanalytics.snowplow.internal.emitter.storage

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.emitter.storage.EventStoreHelper.Companion.getInstance
import com.snowplowanalytics.core.emitter.storage.EventStoreHelper.Companion.removeUnsentEventsExceptForNamespaces
import com.snowplowanalytics.core.emitter.storage.SQLiteEventStore
import com.snowplowanalytics.snowplow.payload.Payload
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.payload.TrackerPayload
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class EventStoreTest {
    @Before
    @Throws(Exception::class)
    fun setUp() {
        for (eventStore in openedEventStores) {
            eventStore.close()
        }
        openedEventStores.clear()
        removeUnsentEventsExceptForNamespaces(InstrumentationRegistry.getContext(), ArrayList())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testAddEventOnEmptyStore() {
        val eventStore = SQLiteEventStore(InstrumentationRegistry.getContext(), "namespace")
        openedEventStores.add(eventStore)
        eventStore.add(payload())
        Assert.assertFalse(eventStore.isDatabaseOpen)
        Assert.assertEquals(0, eventStore.getEmittableEvents(QUERY_LIMIT).size.toLong())
        waitUntilDatabaseOpen(eventStore)
        Assert.assertTrue(eventStore.isDatabaseOpen)
        Assert.assertEquals(1, eventStore.getEmittableEvents(QUERY_LIMIT).size.toLong())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testAddEventOnNotEmptyStore() {
        var eventStore = SQLiteEventStore(InstrumentationRegistry.getContext(), "namespace")
        openedEventStores.add(eventStore)

        // fill eventStore with 1 event
        val trackerPayload = TrackerPayload()
        trackerPayload.add("k", "v")
        eventStore.add(trackerPayload)
        waitUntilDatabaseOpen(eventStore)
        val list = eventStore.getEmittableEvents(QUERY_LIMIT)
        Assert.assertEquals(1, eventStore.getEmittableEvents(QUERY_LIMIT).size.toLong())
        eventStore.close()

        // add new event
        eventStore = SQLiteEventStore(InstrumentationRegistry.getContext(), "namespace")
        openedEventStores.add(eventStore)
        eventStore.add(payload())
        Assert.assertFalse(eventStore.isDatabaseOpen)
        Assert.assertEquals(0, eventStore.getEmittableEvents(QUERY_LIMIT).size.toLong())
        waitUntilDatabaseOpen(eventStore)
        Assert.assertTrue(eventStore.isDatabaseOpen)
        Assert.assertEquals(2, eventStore.getEmittableEvents(QUERY_LIMIT).size.toLong())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testRemoveEventsOnNotEmptyStore() {
        var eventStore = SQLiteEventStore(InstrumentationRegistry.getContext(), "namespace")
        openedEventStores.add(eventStore)

        // fill eventStore with 1 event
        eventStore.add(payload())
        waitUntilDatabaseOpen(eventStore)
        Assert.assertEquals(1, eventStore.getEmittableEvents(QUERY_LIMIT).size.toLong())
        eventStore.close()

        // add new event and remove when database closed
        eventStore = SQLiteEventStore(InstrumentationRegistry.getContext(), "namespace")
        openedEventStores.add(eventStore)
        eventStore.add(payload())
        Assert.assertEquals(1, eventStore.size())
        eventStore.removeAllEvents()
        Assert.assertEquals(0, eventStore.size())
        Assert.assertFalse(eventStore.isDatabaseOpen)
        waitUntilDatabaseOpen(eventStore)
        Assert.assertTrue(eventStore.isDatabaseOpen)
        Assert.assertEquals(1, eventStore.getEmittableEvents(QUERY_LIMIT).size.toLong())
    }

    // Tests with database already open
    @Test
    @Throws(InterruptedException::class)
    fun testGetNonExistentEvent() {
        val eventStore = eventStore()
        var event: Map<String, Any?>? = eventStore.getEvent(-1)
        Assert.assertNull(event)
        eventStore.close()
        event = eventStore.getEvent(-1)
        Assert.assertNull(event)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testInsertPayload() {
        val eventStore = eventStore()
        val id = eventStore.insertEvent(payload())
        val lastRowId = eventStore.lastInsertedRowId
        val event = eventStore.getEvent(id)
        Assert.assertEquals(id, lastRowId)
        Assert.assertEquals(1, eventStore.size())
        Assert.assertNotNull(event)
        
        eventStore.close()
        eventStore.insertEvent(payload())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testRemoveAllEvents() {
        val eventStore = eventStore()
        eventStore.add(payload())
        eventStore.add(payload())
        eventStore.add(payload())
        eventStore.add(payload())
        eventStore.add(payload())
        eventStore.add(payload())
        
        Assert.assertEquals(6, eventStore.size())
        eventStore.removeAllEvents()
        Assert.assertEquals(0, eventStore.size())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testRemoveIndividualEvent() {
        val eventStore = eventStore()
        val id = eventStore.insertEvent(payload())
        var res = eventStore.removeEvent(id)
        Assert.assertEquals(0, eventStore.size())
        Assert.assertTrue(res)
        res = eventStore.removeEvent(id)
        Assert.assertFalse(res)
        eventStore.close()
        res = eventStore.removeEvent(id)
        Assert.assertFalse(res)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testRemoveRangeOfEvents() {
        val eventStore = eventStore()
        val idList: MutableList<Long?> = ArrayList()
        idList.add(eventStore.insertEvent(payload()))
        idList.add(eventStore.insertEvent(payload()))
        idList.add(eventStore.insertEvent(payload()))
        Assert.assertEquals(3, idList.size.toLong())
        Assert.assertEquals(3, eventStore.size())
        var res = eventStore.removeEvents(idList)
        Assert.assertEquals(0, eventStore.size())
        Assert.assertEquals(true, res)
        res = eventStore.removeEvents(idList)
        Assert.assertEquals(false, res)
        eventStore.close()
        res = eventStore.removeEvents(idList)
        Assert.assertEquals(false, res)
        res = eventStore.removeEvents(ArrayList())
        Assert.assertEquals(false, res)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testCloseDatabase() {
        val eventStore = eventStore()
        Assert.assertTrue(eventStore.isDatabaseOpen)
        eventStore.close()
        Assert.assertFalse(eventStore.isDatabaseOpen)
    }

    @Test
    fun testUpgrade() {
        val helper = getInstance(InstrumentationRegistry.getContext(), "namespace")
        val database = helper.writableDatabase
        helper.onUpgrade(database, 1, 2)
        helper.close()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testEventStoreCreateDatabase() {
        val context = InstrumentationRegistry.getContext()
        val eventStore = SQLiteEventStore(context, "namespace")
        openedEventStores.add(eventStore)
        waitUntilDatabaseOpen(eventStore)
        val databaseList = Arrays.asList(*context.databaseList())
        Assert.assertTrue(databaseList.contains("snowplowEvents-namespace.sqlite"))
        Assert.assertTrue(databaseList.contains("snowplowEvents-namespace.sqlite-wal"))
        Assert.assertTrue(databaseList.contains("snowplowEvents-namespace.sqlite-shm"))
    }

    @Test
    @Throws(InterruptedException::class)
    fun testEventStoreRemoveDatabases() {
        val context = InstrumentationRegistry.getContext()
        val eventStore1 = SQLiteEventStore(context, "namespace1")
        val eventStore2 = SQLiteEventStore(context, "namespace2")
        val eventStore3 = SQLiteEventStore(context, "namespace3")
        openedEventStores.addAll(Arrays.asList(eventStore1, eventStore2, eventStore3))
        waitUntilDatabaseOpen(eventStore1)
        waitUntilDatabaseOpen(eventStore2)
        waitUntilDatabaseOpen(eventStore3)
        // Remove database
        removeUnsentEventsExceptForNamespaces(context, listOf("namespace2"))
        val databaseList = Arrays.asList(*context.databaseList())
        Assert.assertFalse(databaseList.contains("snowplowEvents-namespace1.sqlite"))
        Assert.assertFalse(databaseList.contains("snowplowEvents-namespace1.sqlite-wal"))
        Assert.assertFalse(databaseList.contains("snowplowEvents-namespace1.sqlite-shm"))
        Assert.assertTrue(databaseList.contains("snowplowEvents-namespace2.sqlite"))
        Assert.assertTrue(databaseList.contains("snowplowEvents-namespace2.sqlite-wal"))
        Assert.assertTrue(databaseList.contains("snowplowEvents-namespace2.sqlite-shm"))
        Assert.assertFalse(databaseList.contains("snowplowEvents-namespace3.sqlite"))
        Assert.assertFalse(databaseList.contains("snowplowEvents-namespace3.sqlite-wal"))
        Assert.assertFalse(databaseList.contains("snowplowEvents-namespace3.sqlite-shm"))
    }

    @Test
    @Throws(InterruptedException::class)
    fun testEventStoreInvalidNamespaceConversion() {
        val context = InstrumentationRegistry.getContext()
        val eventStore = SQLiteEventStore(context, "namespace*.^?1ò2@")
        openedEventStores.add(eventStore)
        waitUntilDatabaseOpen(eventStore)
        val databaseList = Arrays.asList(*context.databaseList())
        Assert.assertTrue(databaseList.contains("snowplowEvents-namespace-1-2-.sqlite"))
    }

    @Test
    @Throws(InterruptedException::class)
    fun testMigrationFromLegacyToNamespacedEventStore() {
        val context = InstrumentationRegistry.getContext()

        // Create fake legacy database
        val legacyDB: SQLiteOpenHelper =
            object : SQLiteOpenHelper(context, "snowplowEvents.sqlite", null, 1) {
                override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
                    sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS 'events' (id INTEGER PRIMARY KEY)")
                }

                override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, i: Int, i1: Int) {}
            }
        val db = legacyDB.writableDatabase
        db.enableWriteAheadLogging()
        legacyDB.close()
        var databaseList = Arrays.asList(*context.databaseList())
        // old DB
        Assert.assertTrue(databaseList.contains("snowplowEvents.sqlite"))
        // old DB is closed so these should be false
        Assert.assertFalse(databaseList.contains("snowplowEvents.sqlite-wal"))
        Assert.assertFalse(databaseList.contains("snowplowEvents.sqlite-shm"))
        // new DB not yet created so these should be false
        Assert.assertFalse(databaseList.contains("snowplowEvents-namespace.sqlite"))
        Assert.assertFalse(databaseList.contains("snowplowEvents-namespace.sqlite-wal"))
        Assert.assertFalse(databaseList.contains("snowplowEvents-namespace.sqlite-shm"))

        // Migrate database when SQLiteEventStore is launched the first time
        val eventStore = SQLiteEventStore(context, "namespace")
        openedEventStores.add(eventStore)
        waitUntilDatabaseOpen(eventStore)
        databaseList = Arrays.asList(*context.databaseList())
        Assert.assertFalse(databaseList.contains("snowplowEvents.sqlite"))
        Assert.assertFalse(databaseList.contains("snowplowEvents.sqlite-wal"))
        Assert.assertFalse(databaseList.contains("snowplowEvents.sqlite-shm"))
        Assert.assertTrue(databaseList.contains("snowplowEvents-namespace.sqlite"))
        Assert.assertTrue(databaseList.contains("snowplowEvents-namespace.sqlite-wal"))
        Assert.assertTrue(databaseList.contains("snowplowEvents-namespace.sqlite-shm"))
    }

    @Test
    @Throws(InterruptedException::class)
    fun testMultipleAccessToSameSQLiteFile() {
        val context = InstrumentationRegistry.getContext()
        val eventStore1 = SQLiteEventStore(context, "namespace")
        openedEventStores.add(eventStore1)
        waitUntilDatabaseOpen(eventStore1)
        var trackerPayload = TrackerPayload()
        trackerPayload.add("key1", "value1")
        eventStore1.insertEvent(trackerPayload)
        Assert.assertEquals(1, eventStore1.size())
        val eventStore2 = SQLiteEventStore(context, "namespace")
        openedEventStores.add(eventStore2)
        waitUntilDatabaseOpen(eventStore2)
        trackerPayload = TrackerPayload()
        trackerPayload.add("key2", "value2")
        eventStore2.insertEvent(trackerPayload)
        Assert.assertEquals(2, eventStore2.size())
    }

    // Helper Methods

    @Throws(InterruptedException::class)
    private fun eventStore(): SQLiteEventStore {
        val eventStore = SQLiteEventStore(InstrumentationRegistry.getContext(), "namespace")
        openedEventStores.add(eventStore)
        waitUntilDatabaseOpen(eventStore)
        eventStore.removeAllEvents()
        return eventStore
    }
    
    private fun payload(): Payload {
        val trackerPayload = TrackerPayload()
        trackerPayload.add("someKey", "someValue")
        trackerPayload.add("anotherKey", "anotherValue")
        val event = SelfDescribingJson(
            "iglu:com.snowplowanalytics.snowplow/example/jsonschema/1-0-0",
            trackerPayload
        )
        val result = TrackerPayload()
        result.addMap(event.map)
        return result
    }

    @Throws(InterruptedException::class)
    private fun waitUntilDatabaseOpen(eventStore: SQLiteEventStore) {
        while (!eventStore.isDatabaseOpen) {
            Thread.sleep(300)
        }
    }

    companion object {
        var QUERY_LIMIT = 150
        private val openedEventStores: MutableList<SQLiteEventStore> = ArrayList()
    }
}
