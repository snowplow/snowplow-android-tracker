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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.Scheduler;

import com.snowplowanalytics.snowplow.tracker.Payload;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.utils.storage.EmittableEvents;

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

    private final Scheduler scheduler = Schedulers.io();

    /**
     * Creates a new Event Store
     *
     * @param context The android context object
     */
    public EventStore(Context context) {
        dbHelper = new EventStoreHelper(context);
        open();
        Logger.ifDebug(TAG, "DB Path: " + database.getPath());
    }

    /**
     * Opens a new writable database and
     * sets the database to allow WAL.
     *
     * WAL: https://www.sqlite.org/wal.html
     *
     * @return success or failure to open
     */
    public boolean open() {

        // Opens the database
        database = dbHelper.getWritableDatabase();

        // Enable write ahead logging
        database.enableWriteAheadLogging();
        return database != null;
    }

    /**
     * Closes the database
     */
    public void close() {
        dbHelper.close();
    }

    /**
     * Creates a new subscription to an observable
     * operation which is loaded into a buffered
     * queue.
     *
     * @param payload the event payload that is
     *                being added.
     */
    public void add(Payload payload) {
        addObservable(payload)
            .subscribeOn(scheduler)
            .unsubscribeOn(scheduler)
            .subscribe();
    }

    /**
     * An observable object for inserting an event
     * into the database.
     *
     * @param payload the event payload that is
     *                being added.
     * @return an observable event add
     */
    private Observable<Long> addObservable(final Payload payload) {
        return Observable.<Long>create(subscriber -> {
            subscriber.onNext(insertEvent(payload));
            subscriber.onCompleted();
        })
        .onBackpressureBuffer(TrackerConstants.BACK_PRESSURE_LIMIT);
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
        if (open()) {
            byte[] bytes = EventStore.serialize(payload.getMap());
            ContentValues values = new ContentValues(2);
            values.put(EventStoreHelper.COLUMN_EVENT_DATA, bytes);
            lastInsertedRowId = database.insert(EventStoreHelper.TABLE_EVENTS, null, values);
        }

        Logger.ifDebug(TAG, "Added event too database: %s", lastInsertedRowId);

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
        if (open()) {
            retval = database.delete(EventStoreHelper.TABLE_EVENTS,
                    EventStoreHelper.COLUMN_ID + "=" + id, null);
        }

        Logger.ifDebug(TAG, "Removed event from database: %s", "" + id);

        return retval == 1;
    }

    /**
     * Empties the database of all events
     *
     * @return a boolean of success to remove
     */
    public boolean removeAllEvents() {
        int retval = -1;
        if (open()) {
            retval = database.delete(EventStoreHelper.TABLE_EVENTS, null, null);
        }
        return retval == 0;
    }

    /**
     * Converts an event map to a byte
     * array for storage.
     *
     * @param map the map containing all
     *            the event parameters
     * @return the byte array or null
     */
    private static byte[] serialize(Map<String, String> map) {
        try {
            ByteArrayOutputStream mem_out = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(mem_out);

            out.writeObject(map);

            out.close();
            mem_out.close();

            return mem_out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Converts a byte array back into an
     * event map for sending.
     *
     * @param bytes the bytes to be converted
     * @return the Map or null
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> deserializer(byte[] bytes) {
        try {
            ByteArrayInputStream mem_in = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(mem_in);
            Map<String, String> map = (HashMap<String, String>) in.readObject();

            in.close();
            mem_in.close();

            return map;
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the events that validate a
     * specific query.
     *
     * @param query the query to be passed against
     *              the database
     * @return the list of events that satisfied
     * the query
     */
    public List<Map<String, Object>> queryDatabase(String query, String orderBy) {
        List<Map<String, Object>> res = new ArrayList<>();
        if (open()) {
            Cursor cursor = database.query(EventStoreHelper.TABLE_EVENTS, allColumns, query,
                    null, null, null, orderBy);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Map<String, Object> eventMetadata = new HashMap<>();
                eventMetadata.put(EventStoreHelper.METADATA_ID, cursor.getLong(0));
                eventMetadata.put(EventStoreHelper.METADATA_EVENT_DATA,
                        EventStore.deserializer(cursor.getBlob(1)));
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

        // LinkedList of eventIds
        LinkedList<Long> eventIds = new LinkedList<>();

        // ArrayList of event payloads
        ArrayList<Payload> events = new ArrayList<>();

        // FIFO Pattern for sending events
        for (Map<String, Object> eventMetadata :
                getDescEventsInRange(TrackerConstants.EMITTER_SEND_LIMIT)) {

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

        // Return an Events object containing the rowIds and the event payloads
        return new EmittableEvents(events, eventIds);
    }

    /**
     * Returns a Map<String, String> containing the
     * event payload values, the table row ID and
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
        }
        else {
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
        return database.isOpen();
    }
}
