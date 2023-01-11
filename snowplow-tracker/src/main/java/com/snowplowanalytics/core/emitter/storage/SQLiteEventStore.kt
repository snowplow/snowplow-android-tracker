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
package com.snowplowanalytics.core.emitter.storage

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase

import com.snowplowanalytics.core.emitter.Executor
import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.core.utils.Util
import com.snowplowanalytics.snowplow.emitter.EmitterEvent
import com.snowplowanalytics.snowplow.emitter.EventStore
import com.snowplowanalytics.snowplow.payload.Payload
import com.snowplowanalytics.snowplow.payload.TrackerPayload

/**
 * Helper class for storing, getting and removing
 * events from the SQLite database.
 * 
 * @param context The android context object
 *
 * @apiNote The database has a long startup time.
 * The events added to the store are placed in a
 * temporary list meanwhile the database is not
 * yet ready.
 */
class SQLiteEventStore(context: Context, private val namespace: String) : EventStore {
    private val payloadWaitingList: MutableList<Payload> = ArrayList()
    private var database: SQLiteDatabase? = null
    private lateinit var dbHelper: EventStoreHelper
    private val allColumns = arrayOf(
        EventStoreHelper.COLUMN_ID,
        EventStoreHelper.COLUMN_EVENT_DATA,
        EventStoreHelper.COLUMN_DATE_CREATED
    )

    /**
     * Returns the last rowId to be inserted.
     *
     * @return the last inserted rowId, -1 if the database is not open yet.
     */
    var lastInsertedRowId: Long = -1
        private set

    /**
     * Creates a new Event Store
     */
    init {
        Executor.futureCallable {
            dbHelper = EventStoreHelper.getInstance(context, namespace)
            open()
            Logger.d(TAG, "DB Path: %s", database?.path)
            null
        }
    }

    override fun add(payload: Payload) {
        if (!isDatabaseOpen) {
            synchronized(this) { payloadWaitingList.add(payload) }
        } else {
            insertWaitingEventsIfReady()
            insertEvent(payload)
        }
    }

    /**
     * Opens a new writable database if it
     * is currently closed.
     */
    fun open() {
        if (!isDatabaseOpen) {
            database = dbHelper.writableDatabase
            database?.enableWriteAheadLogging()
        }
    }

    /**
     * Closes the database
     */
    fun close() {
        dbHelper.close()
        EventStoreHelper.removeInstance(namespace)
    }

    /**
     * Inserts a payload into the database
     *
     * @param payload The event payload to
     * be stored
     * @return a boolean stating if the insert
     * was a success or not
     */
    fun insertEvent(payload: Payload): Long {
        if (isDatabaseOpen) {
            val bytes = Util.serialize(Util.objectMapToString(payload.map))
            val values = ContentValues(2)
            values.put(EventStoreHelper.COLUMN_EVENT_DATA, bytes)
            lastInsertedRowId =
                database!!.insert(EventStoreHelper.TABLE_EVENTS, null, values)
        }
        Logger.d(TAG, "Added event to database: %s", lastInsertedRowId)
        return lastInsertedRowId
    }

    override fun removeEvent(id: Long): Boolean {
        var retval = -1
        if (isDatabaseOpen) {
            retval = database!!.delete(
                EventStoreHelper.TABLE_EVENTS,
                EventStoreHelper.COLUMN_ID + "=" + id, null
            )
        }
        Logger.d(TAG, "Removed event from database: %s", "" + id)
        return retval == 1
    }

    override fun removeEvents(ids: List<Long>): Boolean {
        if (ids.isEmpty()) {
            return false
        }
        var retval = -1
        if (isDatabaseOpen) {
            retval = database!!.delete(
                EventStoreHelper.TABLE_EVENTS,
                EventStoreHelper.COLUMN_ID + " in (" + Util.joinLongList(ids) + ")", null
            )
        }
        Logger.d(TAG, "Removed events from database: %s", retval)
        return retval == ids.size
    }

    override fun removeAllEvents(): Boolean {
        var retval = 0
        Logger.d(TAG, "Removing all events from database.")
        if (isDatabaseOpen) {
            retval = database!!.delete(EventStoreHelper.TABLE_EVENTS, null, null)
        } else {
            Logger.e(TAG, "Database is not open.")
        }
        retval += payloadWaitingList.size
        payloadWaitingList.clear()
        return retval >= 0
    }

    /**
     * Returns the events that validate a
     * specific query.
     *
     * @param query the query to be passed against
     * the database
     * @param orderBy what to order the query by
     * @return the list of events that satisfied
     * the query
     */
    private fun queryDatabase(query: String?, orderBy: String?): List<Map<String, Any?>> {
        val res: MutableList<Map<String, Any?>> = ArrayList()
        if (isDatabaseOpen) {
            var cursor: Cursor? = null
            try {
                cursor = database!!.query(
                    EventStoreHelper.TABLE_EVENTS,
                    allColumns,
                    query,
                    null,
                    null,
                    null,
                    orderBy
                )
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val eventMetadata: MutableMap<String, Any?> = HashMap()
                    eventMetadata[EventStoreHelper.METADATA_ID] = cursor.getLong(0)
                    eventMetadata[EventStoreHelper.METADATA_EVENT_DATA] =
                        Util.deserializer(cursor.getBlob(1))
                    eventMetadata[EventStoreHelper.METADATA_DATE_CREATED] =
                        cursor.getString(2)
                    cursor.moveToNext()
                    res.add(eventMetadata)
                }
            } finally {
                cursor?.close()
            }
        }
        return res
    }

    // Getters
    override val size: Long
        get() = if (isDatabaseOpen) {
            insertWaitingEventsIfReady()
            DatabaseUtils.queryNumEntries(database, EventStoreHelper.TABLE_EVENTS)
        } else {
            payloadWaitingList.size.toLong()
        }

    override fun getEmittableEvents(queryLimit: Int): List<EmitterEvent?> {
        if (!isDatabaseOpen) {
            return emptyList<EmitterEvent>()
        }
        insertWaitingEventsIfReady()
        val events = ArrayList<EmitterEvent?>()

        // FIFO Pattern for sending events
        for (eventMetadata in getDescEventsInRange(queryLimit)) {

            // Create a TrackerPayload for each event
            val payload = TrackerPayload()
            val eventData =
                eventMetadata[EventStoreHelper.METADATA_EVENT_DATA] as Map<String, Any>?
            payload.addMap(eventData)

            // Create EmitterEvent
            val eventId = eventMetadata[EventStoreHelper.METADATA_ID] as Long?
            if (eventId == null) {
                Logger.e(TAG, "Unable to get ID of an event extracted from the database.")
                continue
            }
            val event = EmitterEvent(payload, eventId)
            events.add(event)
        }
        return events
    }

    /**
     * Returns a Map containing the event
     * payload values, the table row ID and
     * the date it was created.
     *
     * @param id the row id of the event to get
     * @return event metadata
     */
    fun getEvent(id: Long): Map<String, Any?>? {
        val res = queryDatabase(EventStoreHelper.COLUMN_ID + "=" + id, null)
        return if (res.isNotEmpty()) {
            res[0]
        } else {
            null
        }
    }

    /**
     * Returns a list of all the events in the
     * database.
     *
     * @return the events in the database
     */
    val allEvents: List<Map<String, Any?>>
        get() = queryDatabase(null, null)

    /**
     * Returns a descending range of events
     * from the top of the database.
     *
     * @param range amount of rows to take
     * @return a list of event metadata
     */
    fun getDescEventsInRange(range: Int): List<Map<String, Any?>> {
        return queryDatabase(null, "id DESC LIMIT $range")
    }

    /**
     * Returns truth on if database is open.
     *
     * @return a boolean for database status
     */
    val isDatabaseOpen: Boolean
        get() = database != null && database!!.isOpen

    private fun insertWaitingEventsIfReady() {
        if (isDatabaseOpen && payloadWaitingList.size > 0) {
            synchronized(this) {
                for (p in payloadWaitingList) {
                    insertEvent(p)
                }
                payloadWaitingList.clear()
            }
        }
    }

    companion object {
        private val TAG = SQLiteEventStore::class.java.simpleName
        
        fun removeUnsentEventsExceptForNamespaces(
            context: Context,
            allowedNamespaces: List<String>?
        ): List<String> {
            return EventStoreHelper.removeUnsentEventsExceptForNamespaces(
                context,
                allowedNamespaces
            )
        }
    }
}
