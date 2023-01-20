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

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.tracker.Logger
import java.io.File

/**
 * Helper class for building and maintaining the SQLite
 * Database used by the Tracker.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class EventStoreHelper
/**
 * @param context the android context
 */
private constructor(context: Context, databaseName: String) :
    SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION) {
    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(queryCreateTable)
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Logger.d(TAG, "Upgrade not implemented, resetting database...")
        database.execSQL(queryDropTable)
        onCreate(database)
    }

    companion object {
        const val TABLE_EVENTS = "events"
        const val COLUMN_ID = "id"
        const val COLUMN_EVENT_DATA = "eventData"
        const val COLUMN_DATE_CREATED = "dateCreated"
        const val METADATA_ID = "id"
        const val METADATA_EVENT_DATA = "eventData"
        const val METADATA_DATE_CREATED = "dateCreated"
        private const val DATABASE_NAME = "snowplowEvents"
        private val TAG = EventStoreHelper::class.java.name
        private const val DATABASE_VERSION = 1
        private const val queryDropTable = "DROP TABLE IF EXISTS '$TABLE_EVENTS'"
        private const val queryCreateTable = "CREATE TABLE IF NOT EXISTS 'events' " +
                "(id INTEGER PRIMARY KEY, eventData BLOB, " +
                "dateCreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"

        // Prevents multiple instances being created and avoids memory leaks.
        private val instances = HashMap<String, EventStoreHelper>()

        /**
         * IMPORTANT:  This method is for internal use only. Its signature and behaviour might change in any
         * future tracker release.
         *
         * Clears all the EventStores not associated at any of the namespaces passed as parameter.
         *
         * @param context The Android app context.
         * @param allowedNamespaces The namespace allowed. All the EventStores not associated at any of
         * the allowedNamespaces will be cleared.
         * @return The list of namespaces that have been found with EventStores and have been cleared out.
         */
        @JvmStatic
        @Synchronized
        fun removeUnsentEventsExceptForNamespaces(
            context: Context,
            allowedNamespaces: List<String>?
        ): List<String> {
            var allowedNamespaces = allowedNamespaces
            
            if (allowedNamespaces == null) {
                allowedNamespaces = ArrayList()
            }
            val databaseList = context.databaseList() ?: return ArrayList()
            val allowedDbFiles: MutableList<String> = ArrayList(allowedNamespaces.size)
            for (namespace in allowedNamespaces) {
                val sqliteSuffix = namespace.replace("[^a-zA-Z0-9_]+".toRegex(), "-")
                val dbName = DATABASE_NAME + "-" + sqliteSuffix + ".sqlite"
                allowedDbFiles.add(dbName)
                allowedDbFiles.add("$dbName-wal")
                allowedDbFiles.add("$dbName-shm")
            }
            val removedDbFiles: MutableList<String> = ArrayList()
            for (dbName in databaseList) {
                if (!dbName.startsWith(DATABASE_NAME)) {
                    continue
                }
                if (!allowedDbFiles.contains(dbName)) {
                    if (context.deleteDatabase(dbName)) {
                        removedDbFiles.add(dbName)
                    }
                }
            }
            return removedDbFiles
        }

        /**
         * Use the application context, which will ensure that you
         * don't accidentally leak an Activity's context.
         * See this article for more information: http://bit.ly/6LRzfx
         *
         * @param context the android context
         * @return the EventStoreHelper instance
         */
        @JvmStatic
        @Synchronized
        fun getInstance(context: Context, namespace: String): EventStoreHelper {
            if (instances.containsKey(namespace)) {
                return instances[namespace]!!
            }
            // Create new database name
            val sqliteSuffix = namespace.replace("[^a-zA-Z0-9_]+".toRegex(), "-")
            val dbName = "$DATABASE_NAME-$sqliteSuffix.sqlite"

            // Migrate old database if it exists
            renameLegacyDatabase(context, dbName)

            // Create database helper
            val eventStoreHelper = EventStoreHelper(context.applicationContext, dbName)
            instances[namespace] = eventStoreHelper
            return eventStoreHelper
        }

        @Synchronized
        fun removeInstance(namespace: String): EventStoreHelper? {
            return instances.remove(namespace)
        }

        private fun renameLegacyDatabase(context: Context, newDatabaseFilename: String): Boolean {
            val database = context.getDatabasePath("snowplowEvents.sqlite")
            val databaseWal = context.getDatabasePath("snowplowEvents.sqlite-wal")
            val databaseShm = context.getDatabasePath("snowplowEvents.sqlite-shm")
            val parentFile = database.parentFile
            val newDatabase = File(parentFile, newDatabaseFilename)
            val newDatabaseWal = File(parentFile, "$newDatabaseFilename-wal")
            val newDatabaseShm = File(parentFile, "$newDatabaseFilename-shm")
            return (database.renameTo(newDatabase)
                    && databaseWal.renameTo(newDatabaseWal)
                    && databaseShm.renameTo(newDatabaseShm))
        }
    }
}
