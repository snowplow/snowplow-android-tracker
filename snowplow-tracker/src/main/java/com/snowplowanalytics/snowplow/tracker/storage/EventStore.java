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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.emitter.EmittableEvents;
import com.snowplowanalytics.snowplow.tracker.utils.Util;

/**
 * Helper class for storing, getting and removing
 * events from the SQLite database.
 */
public class EventStore {

    private String TAG = EventStore.class.getSimpleName();

    private SQLiteDatabase database;
    private EventStoreHelper dbHelper;
    private String[] allColumns = {
            EventStoreHelper.COLUMN_ID,
            EventStoreHelper.COLUMN_EVENT_DATA,
            EventStoreHelper.COLUMN_DATE_CREATED
    };
    private long lastInsertedRowId = -1;

    private int sendLimit;

    /**
     * Creates a new Event Store
     *
     * @param context The android context object
     * @param sendLimit The maximum amount of events that can be sent
     *                  concurrently
     */
    public EventStore(Context context, int sendLimit) {
        dbHelper = EventStoreHelper.getInstance(context);
        open();
        this.sendLimit = sendLimit;

        Logger.d(TAG, "DB Path: %s", database.getPath());
    }

    /**
     * Adds an event to the database.
     *
     * @param payload the payload to be added
     */
    public void add(Payload payload) {
        insertEvent(payload);
    }

    /**
     * Opens a new writable database if it
     * is currently closed.
     */
    public void open() {
        if (!isDatabaseOpen()) {
            database = dbHelper.getWritableDatabase();
            database.enableWriteAheadLogging();
        }
    }

    /**
     * Closes the database
     */
    public void close() {
        dbHelper.close();
    }

    /**
     * Inserts a payload into the database
     *
     * @param payload The event payload to
     *                be stored
     * @return a boolean stating if the insert
     * was a success or not
     */
    @SuppressWarnings("unchecked")
    public long insertEvent(Payload payload) {
        if (isDatabaseOpen()) {
            byte[] bytes = Util.serialize(payload.getMap());
            ContentValues values = new ContentValues(2);
            values.put(EventStoreHelper.COLUMN_EVENT_DATA, bytes);
            lastInsertedRowId = database.insert(EventStoreHelper.TABLE_EVENTS, null, values);
        }
        Logger.d(TAG, "Added event to database: %s", lastInsertedRowId);
        return lastInsertedRowId;
    }

    /**
     * Removes an event from the database
     *
     * @param id the row id of the event
     * @return a boolean of success to remove
     */
    public boolean removeEvent(long id) {
        int retval = -1;
        if (isDatabaseOpen()) {
            retval = database.delete(EventStoreHelper.TABLE_EVENTS,
                    EventStoreHelper.COLUMN_ID + "=" + id, null);
        }
        Logger.d(TAG, "Removed event from database: %s", "" + id);
        return retval == 1;
    }

    /**
     * Removes a range of events from the database
     *
     * @param ids the ids to remove
     * @return a boolean of success to remove
     */
    public boolean removeEvents(List<Long> ids) {
        if (ids.size() == 0) {
            return false;
        }

        int retval = -1;
        if (isDatabaseOpen()) {
            retval = database.delete(EventStoreHelper.TABLE_EVENTS,
                    EventStoreHelper.COLUMN_ID + " in (" + (Util.joinLongList(ids)) + ")", null);
        }
        Logger.d(TAG, "Removed events from database: %s", retval);
        return retval == ids.size();
    }

    /**
     * Empties the database of all events
     *
     * @return a boolean of success to remove
     */
    public boolean removeAllEvents() {
        int retval = -1;
        if (isDatabaseOpen()) {
            retval = database.delete(EventStoreHelper.TABLE_EVENTS, null, null);
        }
        Logger.d(TAG, "Removing all events from database.");
        return retval == 0;
    }

    /**
     * Returns the events that validate a
     * specific query.
     *
     * @param query the query to be passed against
     *              the database
     * @param orderBy what to order the query by
     * @return the list of events that satisfied
     * the query
     */
    private List<Map<String, Object>> queryDatabase(String query, String orderBy) {
        List<Map<String, Object>> res = new ArrayList<>();
        if (isDatabaseOpen()) {
            Cursor cursor = database.query(EventStoreHelper.TABLE_EVENTS, allColumns, query,
                    null, null, null, orderBy);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Map<String, Object> eventMetadata = new HashMap<>();
                eventMetadata.put(EventStoreHelper.METADATA_ID, cursor.getLong(0));
                eventMetadata.put(EventStoreHelper.METADATA_EVENT_DATA,
                        Util.deserializer(cursor.getBlob(1)));
                eventMetadata.put(EventStoreHelper.METADATA_DATE_CREATED, cursor.getString(2));
                cursor.moveToNext();
                res.add(eventMetadata);
            }
            cursor.close();
        }
        return res;
    }

    // Getters

    /**
     * Returns amount of events currently
     * in the database.
     *
     * @return the count of events in the
     * database
     */
    public long getSize() {
        return DatabaseUtils.queryNumEntries(database, EventStoreHelper.TABLE_EVENTS);
    }

    /**
     * Returns the last rowId to be
     * inserted.
     *
     * @return the last inserted rowId
     */
    public long getLastInsertedRowId() {
        return lastInsertedRowId;
    }

    /**
     * Returns an EmittableEvents object which
     * contains events and eventIds within a
     * defined range of the database.
     *
     * @return an EmittableEvents object containing
     * eventIds and event payloads.
     */
    @SuppressWarnings("unchecked")
    public EmittableEvents getEmittableEvents() {

        LinkedList<Long> eventIds = new LinkedList<>();
        ArrayList<Payload> events = new ArrayList<>();

        // FIFO Pattern for sending events
        for (Map<String, Object> eventMetadata : getDescEventsInRange(this.sendLimit)) {

            // Create a TrackerPayload for each event
            TrackerPayload payload = new TrackerPayload();
            Map<String, Object> eventData = (Map<String, Object>)
                    eventMetadata.get(EventStoreHelper.METADATA_EVENT_DATA);
            payload.addMap(eventData);

            // Store the eventId
            Long eventId = (Long) eventMetadata.get(EventStoreHelper.METADATA_ID);
            eventIds.add(eventId);

            // Add the payload to the list
            events.add(payload);
        }
        return new EmittableEvents(events, eventIds);
    }

    /**
     * Returns a Map containing the event 
     * payload values, the table row ID and
     * the date it was created.
     *
     * @param id the row id of the event to get
     * @return event metadata
     */
    public Map<String, Object> getEvent(long id) {
        List<Map<String, Object>> res =
                queryDatabase(EventStoreHelper.COLUMN_ID + "=" + id, null);

        if (!res.isEmpty()) {
            return res.get(0);
        } else {
            return null;
        }
    }

    /**
     * Returns a list of all the events in the
     * database.
     *
     * @return the events in the database
     */
    public List<Map<String, Object>> getAllEvents() {
        return queryDatabase(null, null);
    }

    /**
     * Returns a descending range of events
     * from the top of the database.
     *
     * @param range amount of rows to take
     * @return a list of event metadata
     */
    public List<Map<String, Object>> getDescEventsInRange(int range) {
        return queryDatabase(null, "id DESC LIMIT " + range);
    }

    /**
     * Returns truth on if database is open.
     *
     * @return a boolean for database status
     */
    public boolean isDatabaseOpen() {
        return database != null && database.isOpen();
    }
}