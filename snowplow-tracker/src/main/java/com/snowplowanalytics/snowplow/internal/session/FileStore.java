
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

package com.snowplowanalytics.snowplow.internal.session;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.internal.tracker.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Saves Map objects to physical files in a
 * private area designated to the application.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FileStore {

    private static final String TAG = FileStore.class.getSimpleName();

    /**
     * Saves a file to internal storage with a map of
     * key value pairs.
     * - NOTE: This will overwrite a file already in
     *         storage.
     *
     * @param filename The name of the file to be saved.
     * @param objects The KV Pairs to save
     * @param context The android context object
     * @return success statement
     */
    public static boolean saveMapToFile(@NonNull String filename, @NonNull Map objects, @NonNull Context context) {
        FileOutputStream fos;
        try {
            Logger.d(TAG, "Attempting to save: %s", objects);
            fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(objects);
            oos.close();
            Logger.d(TAG, " + Successfully saved KV Pairs to: %s", filename);
            return true;
        } catch(NullPointerException | IOException ioe){
            Logger.e(TAG, " + Exception saving vars map: %s", ioe.getMessage());
        }
        return false;
    }

    /**
     * Returns a map of KV pairs from a file in internal
     * storage.
     *
     * @param filename The name of the file to be retrieved
     * @param context The android context object
     * @return the map of vars or null
     */
    @Nullable
    public synchronized static Map<String, Object> getMapFromFile(@NonNull String filename, @NonNull Context context) {
        try {
            File file = context.getFileStreamPath(filename);
            if (file == null || !file.exists()) {
                return null;
            }
            Logger.d(TAG, "Attempting to retrieve map from: %s", filename);
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Map<String, Object> varsMap = (HashMap<String, Object>) ois.readObject();
            ois.close();
            Logger.d(TAG, " + Retrieved map from file: %s", varsMap);
            return varsMap;
        } catch (IOException | ClassNotFoundException ioe) {
            Logger.e(TAG, " + Exception getting vars map: %s", ioe.getMessage());
        }
        return null;
    }

    /**
     * Deletes a file in internal storage.
     *
     * @param filename The name of the file to be deleted
     * @param context The android context object
     * @return the success of the operation
     */
    public static boolean deleteFile(@NonNull String filename, @NonNull Context context) {
        boolean isSuccess = context.deleteFile(filename);
        Logger.d(TAG, "Deleted %s from internal storage: %s", filename, isSuccess);
        return isSuccess;
    }
}
