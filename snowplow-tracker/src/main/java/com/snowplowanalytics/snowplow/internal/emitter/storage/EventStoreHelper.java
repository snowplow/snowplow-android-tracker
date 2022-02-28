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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.internal.tracker.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Helper class for building and maintaining the SQLite
 * Database used by the Tracker.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class EventStoreHelper extends SQLiteOpenHelper {

    public static final String TABLE_EVENTS         = "events";
    public static final String COLUMN_ID            = "id";
    public static final String COLUMN_EVENT_DATA    = "eventData";
    public static final String COLUMN_DATE_CREATED  = "dateCreated";

    public static final String METADATA_ID          = "id";
    public static final String METADATA_EVENT_DATA  = "eventData";
    public static final String METADATA_DATE_CREATED= "dateCreated";

    private static final String DATABASE_NAME       = "snowplowEvents";
    private static final String TAG                 = EventStoreHelper.class.getName();
    private static final int DATABASE_VERSION       = 1;

    private static final String queryDropTable =
            "DROP TABLE IF EXISTS '" + TABLE_EVENTS + "'";
    private static final String queryCreateTable = "CREATE TABLE IF NOT EXISTS 'events' " +
            "(id INTEGER PRIMARY KEY, eventData BLOB, " +
            "dateCreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

    // Prevents multiple instances being created and avoids memory leaks.
    private static HashMap<String, EventStoreHelper> instances = new HashMap<>();

    /**
     * IMPORTANT:  This method is for internal use only. It's signature and behaviour might change in any
     * future tracker release.
     *
     * Clears all the EventStores not associated at any of the namespaces passed as parameter.
     *
     * @param context The Android app context.
     * @param allowedNamespaces The namespace allowed. All the EventStores not associated at any of
     *                          the allowedNamespaces will be cleared.
     * @return The list of namespaces that have been found with EventStores and have been cleared out.
     */
    @NonNull
    public synchronized static List<String> removeUnsentEventsExceptForNamespaces(@NonNull Context context, @Nullable List<String> allowedNamespaces) {
        if (allowedNamespaces == null) {
            allowedNamespaces = new ArrayList<>();
        }
        String[] databaseList = context.databaseList();
        if (databaseList == null) {
            return new ArrayList<>();
        }
        List<String> allowedDbFiles = new ArrayList<>(allowedNamespaces.size());
        for (String namespace : allowedNamespaces) {
            String sqliteSuffix = namespace.replaceAll("[^a-zA-Z0-9_]+", "-");
            String dbName = DATABASE_NAME + "-" + sqliteSuffix + ".sqlite";
            allowedDbFiles.add(dbName);
            allowedDbFiles.add(dbName + "-wal");
            allowedDbFiles.add(dbName + "-shm");
        }
        List<String> removedDbFiles = new ArrayList<>();
        for (String dbName : databaseList) {
            if (!dbName.startsWith(DATABASE_NAME)) {
                continue;
            }
            if (!allowedDbFiles.contains(dbName)) {
                if (context.deleteDatabase(dbName)) {
                    removedDbFiles.add(dbName);
                }
            }
        }
        return removedDbFiles;
    }

    /**
     * Use the application context, which will ensure that you
     * don't accidentally leak an Activity's context.
     * See this article for more information: http://bit.ly/6LRzfx
     *
     * @param context the android context
     * @return the EventStoreHelper instance
     */
    @NonNull
    public synchronized static EventStoreHelper getInstance(@NonNull Context context, @NonNull String namespace) {
        if (instances.containsKey(namespace)) {
            return instances.get(namespace);
        }
        // Create new database name
        String sqliteSuffix = namespace.replaceAll("[^a-zA-Z0-9_]+", "-");
        String dbName = DATABASE_NAME + "-" + sqliteSuffix + ".sqlite";

        // Migrate old database if it exists
        renameLegacyDatabase(context, dbName);

        // Create database helper
        EventStoreHelper eventStoreHelper = new EventStoreHelper(context.getApplicationContext(), dbName);
        instances.put(namespace, eventStoreHelper);
        return eventStoreHelper;
    }

    @Nullable
    public synchronized static EventStoreHelper removeInstance(@NonNull String namespace) {
        return instances.remove(namespace);
    }

    private static boolean renameLegacyDatabase(@NonNull Context context, String newDatabaseFilename) {
        File database = context.getDatabasePath("snowplowEvents.sqlite");
        File databaseWal = context.getDatabasePath("snowplowEvents.sqlite-wal");
        File databaseShm = context.getDatabasePath("snowplowEvents.sqlite-shm");
        File parentFile = database.getParentFile();
        File newDatabase = new File(parentFile, newDatabaseFilename);
        File newDatabaseWal = new File(parentFile, newDatabaseFilename + "-wal");
        File newDatabaseShm = new File(parentFile, newDatabaseFilename + "-shm");
        return database.renameTo(newDatabase)
                && databaseWal.renameTo(newDatabaseWal)
                && databaseShm.renameTo(newDatabaseShm);
    }

    /**
     * @param context the android context
     */
    private EventStoreHelper(Context context, String databaseName) {
        super(context, databaseName, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase database) {
        database.execSQL(queryCreateTable);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase database, int oldVersion, int newVersion) {
        Logger.d(TAG, "Upgrade not implemented, resetting database...");
        database.execSQL(queryDropTable);
        onCreate(database);
    }
}
