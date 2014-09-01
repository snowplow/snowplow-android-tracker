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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class EventStoreHelper extends SQLiteOpenHelper {

    public static final String TABLE_EVENTS         = "events";
    public static final String COLUMN_ID            = "id";
    public static final String COLUMN_EVENT_DATA    = "eventData";
    public static final String COLUMN_PENDING       = "pending";
    public static final String COLUMN_DATE_CREATED  = "dateCreated";

    public static final String METADATA_ID          = "id";
    public static final String METADATA_EVENT_DATA  = "eventData";
    public static final String METADATA_DATE_CREATED= "dateCreated";

    private static final String DATABASE_NAME       = "snowplowEvents.sqlite";
    private static final String TAG                 = EventStoreHelper.class.getName();
    private static final int DATABASE_VERSION       = 1;

    private static final String queryDropTable =
            "DROP TABLE IF EXISTS '" + TABLE_EVENTS + "'";
    private static final String queryCreateTable = "CREATE TABLE IF NOT EXISTS 'events' " +
            "(id INTEGER PRIMARY KEY, eventData BLOB, pending INTEGER, " +
            "dateCreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

    public EventStoreHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(queryCreateTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ". Destroying old data now..");
        database.execSQL(queryDropTable);
        onCreate(database);
    }
}
