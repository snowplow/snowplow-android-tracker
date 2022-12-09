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
package com.snowplowanalytics.core.session

import android.content.Context
import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.tracker.Logger
import java.io.*

/**
 * Saves Map objects to physical files in a
 * private area designated to the application.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
object FileStore {
    private val TAG = FileStore::class.java.simpleName

    /**
     * Saves a file to internal storage with a map of
     * key value pairs.
     * - NOTE: This will overwrite a file already in
     * storage.
     *
     * @param filename The name of the file to be saved.
     * @param objects The KV Pairs to save
     * @param context The android context object
     * @return success statement
     */
    @JvmStatic
    fun saveMapToFile(filename: String, objects: Map<*, *>, context: Context): Boolean {
        val fos: FileOutputStream
        try {
            Logger.d(TAG, "Attempting to save: %s", objects)
            fos = context.openFileOutput(filename, Context.MODE_PRIVATE)
            val oos = ObjectOutputStream(fos)
            oos.writeObject(objects)
            oos.close()
            Logger.d(TAG, " + Successfully saved KV Pairs to: %s", filename)
            return true
        } catch (ioe: NullPointerException) {
            Logger.e(TAG, " + Exception saving vars map: %s", ioe.message)
        } catch (ioe: IOException) {
            Logger.e(TAG, " + Exception saving vars map: %s", ioe.message)
        }
        return false
    }

    /**
     * Returns a map of KV pairs from a file in internal
     * storage.
     *
     * @param filename The name of the file to be retrieved
     * @param context The android context object
     * @return the map of vars or null
     */
    @JvmStatic
    @Synchronized
    fun getMapFromFile(filename: String, context: Context): MutableMap<String?, Any?>? {
        try {
            val file = context.getFileStreamPath(filename)
            if (file == null || !file.exists()) {
                return null
            }
            Logger.d(TAG, "Attempting to retrieve map from: %s", filename)
            val fis = FileInputStream(file)
            val ois = ObjectInputStream(fis)
            val varsMap: MutableMap<String?, Any?> = ois.readObject() as HashMap<String?, Any?>
            ois.close()
            Logger.d(TAG, " + Retrieved map from file: %s", varsMap)
            return varsMap
        } catch (ioe: IOException) {
            Logger.e(TAG, " + Exception getting vars map: %s", ioe.message)
        } catch (ioe: ClassNotFoundException) {
            Logger.e(TAG, " + Exception getting vars map: %s", ioe.message)
        }
        return null
    }

    /**
     * Deletes a file in internal storage.
     *
     * @param filename The name of the file to be deleted
     * @param context The android context object
     * @return the success of the operation
     */
    @JvmStatic
    fun deleteFile(filename: String, context: Context): Boolean {
        val isSuccess = context.deleteFile(filename)
        Logger.d(TAG, "Deleted %s from internal storage: %s", filename, isSuccess)
        return isSuccess
    }
}
