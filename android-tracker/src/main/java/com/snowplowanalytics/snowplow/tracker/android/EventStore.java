/*
 * Copyright (c) 2014 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.snowplowanalytics.snowplow.tracker.core.payload.Payload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventStore {

    private SQLiteDatabase database;
    private EventStoreHelper dbHelper;
    private String[] allColumns = { EventStoreHelper.COLUMN_ID,
            EventStoreHelper.COLUMN_EVENT_DATA, EventStoreHelper.COLUMN_DATE_CREATED};
    private long lastInsertedRowId = -1;

    private static final String querySelectAll =
            "SELECT * FROM 'events'";
    private static final String querySelectCount =
            "SELECT Count(*) FROM 'events'";
    private static final String queryInsertEvent =
            "INSERT INTO 'events' (eventData, pending) VALUES (?, 0)";
    private static final String querySelectId =
            "SELECT * FROM 'events' WHERE id=?";
    private static final String queryDeleteId =
            "DELETE FROM 'events' WHERE id=?";
    private static final String querySelectPending =
            "SELECT * FROM 'events' WHERE pending=1";
    private static final String querySelectNonPending =
            "SELECT * FROM 'events' WHERE pending=0";
    private static final String querySetPending =
            "UPDATE events SET pending=1 WHERE id=?";
    private static final String querySetNonPending =
            "UPDATE events SET pending=0 WHERE id=?";

    public EventStore(Context context) {
        dbHelper = new EventStoreHelper(context);
        open();
            System.out.println(database.getPath());
    }

    public boolean open() {
        database = dbHelper.getWritableDatabase();
        return database != null;
    }

    public void close() {
        dbHelper.close();
    }

    @SuppressWarnings("unchecked")
    public long insertPayload(Payload payload) {
        return insertMap(payload.getMap());
    }

    public long insertMap(Map<String, String> map) {
        if (open()) {
            byte[] bytes = EventStore.serialize(map);
            ContentValues values = new ContentValues(2);
            values.put(EventStoreHelper.COLUMN_EVENT_DATA, bytes);
            values.put(EventStoreHelper.COLUMN_PENDING, 0);
            lastInsertedRowId = database.insert(EventStoreHelper.TABLE_EVENTS, null, values);
        }
        return lastInsertedRowId;
    }

    public boolean removeEvent(long id) {
        int retval = -1;
        if (open()) {
            retval = database.delete(EventStoreHelper.TABLE_EVENTS,
                    EventStoreHelper.COLUMN_ID + "=" + id, null);
        }
        return retval == 0;
    }

    public boolean removeAllEvents() {
        int retval = -1;
        if (open()) {
            retval = database.delete(EventStoreHelper.TABLE_EVENTS, null, null);
        }
        return retval == 0;
    }

    public boolean setPending(long id) {
        int retval = -1;
        if (open()) {
            ContentValues value = new ContentValues();
            value.put(EventStoreHelper.COLUMN_PENDING, 1);
            retval = database.update(EventStoreHelper.TABLE_EVENTS,
                    value, EventStoreHelper.COLUMN_ID + "=" + id, null);
        }
        return retval == 1; // Update should only affect one row
    }

    public boolean removePending(long id) {
        int retval = -1;
        if (open()) {
            ContentValues value = new ContentValues();
            value.put(EventStoreHelper.COLUMN_PENDING, 0);
            retval = database.update(EventStoreHelper.TABLE_EVENTS,
                    value, EventStoreHelper.COLUMN_ID + "=" + id, null);
        }
        return retval == 1; // Update should only affect one row
    }

    public long size() {
        return DatabaseUtils.queryNumEntries(database, EventStoreHelper.TABLE_EVENTS);
    }

    public Map<String, Object> getEvent(long id) {
        // The Object contains the table row ID, payload (a map<string, string>) as byte[]
        // and the date created..
        Map<String, Object> eventMetadata = new HashMap<String, Object>();
        if (open()) {
            Cursor cursor = database.query(EventStoreHelper.TABLE_EVENTS, allColumns,
                    EventStoreHelper.COLUMN_ID + "=" + id, null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                eventMetadata.put(EventStoreHelper.METADATA_ID, cursor.getLong(0));
                eventMetadata.put(EventStoreHelper.METADATA_EVENT_DATA,
                        EventStore.deserialize(cursor.getBlob(1)));
//                eventMetadata.put(EventStoreHelper.METADATA_DATE_CREATED,
//                        cursor.getString(3));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return eventMetadata;
    }

    public List<Map<String, Object>> getQueryEvents(String query) {
        List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
        if (open()) {
            Cursor cursor = database.query(EventStoreHelper.TABLE_EVENTS, allColumns, query,
                    null, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Map<String, Object> eventMetadata = new HashMap<String, Object>();
                eventMetadata.put(EventStoreHelper.METADATA_ID, cursor.getLong(0));
                eventMetadata.put(EventStoreHelper.METADATA_EVENT_DATA,
                        EventStore.deserialize(cursor.getBlob(1)));
//                eventMetadata.put(EventStoreHelper.METADATA_DATE_CREATED,
//                        Timestamp.valueOf(cursor.getString(3)));
                cursor.moveToNext();
                res.add(eventMetadata);
            }
        }
        return res;
    }

    public List<Map<String, Object>> getAllEvents() {
        return getQueryEvents(null);
    }

    public List<Map<String, Object>> getAllNonPendingEvents() {
        return getQueryEvents(EventStoreHelper.COLUMN_PENDING + "=0");
    }

    public List<Map<String, Object>> getAllPendingEvents() {
        return getQueryEvents(EventStoreHelper.COLUMN_ID + "=1");
    }

    public long getLastInsertedRowId() {
        return lastInsertedRowId;
    }

    public static byte[] serialize(Map<String, String> map) {
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

    @SuppressWarnings("unchecked")
    public static Map<String, String> deserialize(byte[] bytes) {
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
