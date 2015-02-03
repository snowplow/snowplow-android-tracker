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
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.Scheduler;

import com.snowplowanalytics.snowplow.tracker.Payload;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.utils.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.utils.storage.EmittableEvents;

public class EventStore {

    private String TAG = EventStore.class.getSimpleName();

    private final BufferOption option;

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
    public EventStore(Context context, BufferOption option) {
        this.option = option;

        dbHelper = new EventStoreHelper(context);
        open();

        Logger.ifDebug(TAG, "DB Path: " + database.getPath());
    }

    /**
     * Creates a new operation which goes into a
     * queue of operations to be actioned.
     *
     * @param payload the event payload that is
     *                being added.
     */
    public void add(Payload payload) {
        Subscription subscription = insertEventObservable(payload)
            .subscribeOn(scheduler)
            .unsubscribeOn(scheduler)
            .subscribe(result -> {
                Logger.ifDebug(TAG, "Added event to database");

            });
    }

    /**
     * An observable object for
     * inserting an event into the
     * database.
     *
     * @param payload the event payload that is
     *                being added.
     * @return an observable event add
     */
    private Observable<Long> insertEventObservable(final Payload payload) {
        return Observable.<Long>create(subscriber -> {
            subscriber.onNext(insertPayload(payload));
            subscriber.onCompleted();
        })
        .onBackpressureBuffer(10000);
    }

    /**
     * Creates a new operation to remove an
     * event from the database.
     *
     * @param eventId the row id of the event
     *                to be removed.
     */
    public void remove(Long eventId) {
        Subscription subscription = removeEventObservable(eventId)
                .subscribeOn(scheduler)
                .unsubscribeOn(scheduler)
                .subscribe(result -> {
                    Logger.ifDebug(TAG, "Removed event from database");
                });
    }

    /**
     * An observable function too
     * remove an event from the database.
     *
     * @param id the rowId of the event
     * @return an observable event remove
     */
    private Observable<Boolean> removeEventObservable(final long id) {
        return Observable.<Boolean>create(subscriber -> {
            subscriber.onNext(removeEvent(id));
            subscriber.onCompleted();
        })
        .onBackpressureBuffer(10000);
    }

    /**
     * Returns an EmittableEvents object which
     * contains all the eventIds and payloads
     * of the non-pending events in the database.
     *
     * This function has the side-affect of setting
     * the state of the events to Pending - prevents
     * them from being picked up multiple times.
     *
     * @return an EmittableEvents object containing
     * eventIds and event payloads.
     */
    public EmittableEvents getEmittableEvents() {

        // LinkedList of eventIds
        LinkedList<Long> eventIds = new LinkedList<>();

        // ArrayList of event payloads
        ArrayList<Payload> events = new ArrayList<>();

        // LIFO Pattern for sending events
        for (Map<String, Object> eventMetadata : getEventRange(option.getCode())) {

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
     * Opens a new writable database and
     * sets the database to allow WAL.
     *
     * WAL: https://www.sqlite.org/wal.html
     *
     * @return success or failure to open
     */
    public boolean open() {

        // Open the database
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
     * Inserts a payload into the database
     *
     * @param payload The event payload to
     *                be stored
     * @return a boolean stating if the insert
     * was a success or not
     */
    @SuppressWarnings("unchecked")
    public long insertPayload(Payload payload) {
        return insertMap(payload.getMap());
    }

    /**
     * Inserts a map into the database
     *
     * @param map a mapped payload
     * @return the rowId or -1 if it failed to
     * insert into the database
     */
    private long insertMap(Map<String, String> map) {
        if (open()) {
            byte[] bytes = EventStore.serialize(map);
            ContentValues values = new ContentValues(2);
            values.put(EventStoreHelper.COLUMN_EVENT_DATA, bytes);
            lastInsertedRowId = database.insert(EventStoreHelper.TABLE_EVENTS, null, values);
        }
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

        Logger.ifDebug(TAG, "Event Store size after delete: %s", size());

        return retval == 0;
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
     * Returns amount of events currently
     * in the database.
     *
     * @return the count of events in the
     * database
     */
    public long size() {
        return DatabaseUtils.queryNumEntries(database, EventStoreHelper.TABLE_EVENTS);
    }

    /**
     * Returns a Map<String, String> containing the
     * event payload values, the table row ID and
     * the date it was created.
     *
     * @param id the row id of the event to get
     * @return event metaData
     */
    public Map<String, Object> getEvent(long id) {
        Map<String, Object> eventMetadata = new HashMap<>();
        if (open()) {
            Cursor cursor = database.query(EventStoreHelper.TABLE_EVENTS, allColumns,
                    EventStoreHelper.COLUMN_ID + "=" + id, null, null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                eventMetadata.put(EventStoreHelper.METADATA_ID, cursor.getLong(0));
                eventMetadata.put(EventStoreHelper.METADATA_EVENT_DATA,
                        EventStore.deserializer(cursor.getBlob(1)));
                eventMetadata.put(EventStoreHelper.METADATA_DATE_CREATED,
                        cursor.getString(2));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return eventMetadata;
    }

    /**
     * Returns the events that validate a
     * specific query.
     *
     * Such as returning all pending events
     * -> EventStoreHelper.COLUMN_PENDING + "=1"
     *
     * @param query the query to be passed against
     *              the database
     * @return the list of events that satisfied
     * the query
     */
    public List<Map<String, Object>> getQueryEvents(String query, String orderBy) {
        List<Map<String, Object>> res = new ArrayList<>();
        if (open()) {
            Cursor cursor = database.query(EventStoreHelper.TABLE_EVENTS, allColumns, query,
                    null, null, null, orderBy);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Map<String, Object> eventMetadata = new HashMap<String, Object>();
                eventMetadata.put(EventStoreHelper.METADATA_ID, cursor.getLong(0));
                eventMetadata.put(EventStoreHelper.METADATA_EVENT_DATA,
                        EventStore.deserializer(cursor.getBlob(1)));
                eventMetadata.put(EventStoreHelper.METADATA_DATE_CREATED,
                        cursor.getString(2));
                cursor.moveToNext();
                res.add(eventMetadata);
            }
        }
        return res;
    }

    /**
     * Returns an ascending range of events
     * from the bottom of the database.
     *
     * @param range amount of rows to grab
     * @return the event metadata
     */
    public List<Map<String, Object>> getEventRange(int range) {
        Logger.ifDebug(TAG, "Getting range of events...");
        return getQueryEvents(null, "id ASC LIMIT " + range);
    }

    /**
     * Returns a list of all the events in the
     * database.
     *
     * @return the events in the database
     */
    public List<Map<String, Object>> getAllEvents() {
        return getQueryEvents(null, null);
    }

    /**
     * Returns the last rowId to be
     * inserted.
     *
     * @return a rowId
     */
    public long getLastInsertedRowId() {
        return lastInsertedRowId;
    }

    /**
     * Serializes an event map to a
     * byte array for storage.
     *
     * @param map the map containing all
     *            the event parameters
     * @return the byte[] or null
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
     * Converts a byte[] back into a
     * Map of parameters
     *
     * @param bytes the bytes to be
     *              converted
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
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
