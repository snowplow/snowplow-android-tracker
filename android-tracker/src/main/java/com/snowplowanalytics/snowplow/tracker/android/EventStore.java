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
import java.util.HashMap;
import java.util.Map;

public class EventStore {

    private SQLiteDatabase database;
    private EventStoreHelper dbHelper;
    private String[] allColumns = { EventStoreHelper.COLUMN_ID,
            EventStoreHelper.COLUMN_EVENT_DATA };

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
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long insertPayload(Payload payload) {
        return insertMap(payload.getMap());
    }

    public long insertMap(Map<String, String> map) {
        byte[] bytes = EventStore.serialize(map);
        ContentValues values = new ContentValues(2);
        values.put(EventStoreHelper.COLUMN_EVENT_DATA, bytes);
        values.put(EventStoreHelper.COLUMN_PENDING, 0);
        return database.insert(EventStoreHelper.TABLE_EVENTS, null, values);
    }

    public boolean removeEventWithId(long id) {
        int retval = database.delete(EventStoreHelper.TABLE_EVENTS,
                EventStoreHelper.COLUMN_ID + "=" + id, null);
        return retval == 0;
    }

    public boolean removeAllEvents() {
        int retval = database.delete(EventStoreHelper.TABLE_EVENTS, null, null);
        return retval == 0;
    }

    public boolean setPending(long id) {
        ContentValues value = new ContentValues();
        value.put(EventStoreHelper.COLUMN_PENDING, 1);
        int retval = database.update(EventStoreHelper.TABLE_EVENTS,
                value, EventStoreHelper.COLUMN_ID + "=" + id, null);
        return retval == 1; // Update should only affect one row
    }

    public boolean removePending(long id) {
        ContentValues value = new ContentValues();
        value.put(EventStoreHelper.COLUMN_PENDING, 0);
        int retval = database.update(EventStoreHelper.TABLE_EVENTS,
                value, EventStoreHelper.COLUMN_ID + "=" + id, null);
        return retval == 1; // Update should only affect one row
    }

    public long size() {
        return DatabaseUtils.queryNumEntries(database, EventStoreHelper.TABLE_EVENTS);
    }

    public Map<String, Object> getEvent(long id) {
        Map<String, Object> eventMetadata = new HashMap<String, Object>();
        Cursor cursor = database.query(EventStoreHelper.TABLE_EVENTS, allColumns,
                null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            eventMetadata.put(EventStoreHelper.METADATA_ID, cursor.getLong(0));
            eventMetadata.put(EventStoreHelper.METADATA_EVENT_DATA, cursor.getBlob(1));
            eventMetadata.put(EventStoreHelper.METADATA_DATE_CREATED, cursor.getInt(2));
        }
        cursor.close();
        return eventMetadata;
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
