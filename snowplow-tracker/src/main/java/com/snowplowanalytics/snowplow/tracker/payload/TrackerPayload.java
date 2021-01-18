/*
 * Copyright (c) 2015-2020 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker.payload;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import com.snowplowanalytics.snowplow.tracker.utils.Util;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;

/**
 * Returns a standard Tracker Payload consisting of
 * many key - pair values.
 */
public class TrackerPayload implements Payload {

    private final String TAG = TrackerPayload.class.getSimpleName();
    private final HashMap<String,Object> payload = new HashMap<>();
    
    @Override
    public void add(@NonNull String key, @Nullable String value) {
        if (value == null || value.isEmpty()) {
            Logger.v(TAG, "The keys value is empty, removing the key: %s", key);
            payload.remove(key);
            return;
        }
        Logger.v(TAG, "Adding new kv pair: " + key + "->%s", value);
        payload.put(key, value);
    }

    @Override
    public void add(@NonNull String key, @Nullable Object value) {
        if (value == null) {
            Logger.v(TAG, "The value is empty, removing the key: %s", key);
            payload.remove(key);
            return;
        }
        Logger.v(TAG, "Adding new kv pair: " + key + "->%s", value);
        payload.put(key, value);
    }

    @Override
    public void addMap(@NonNull Map<String, Object> map) {
        if (map == null) {
            Logger.v(TAG, "Map passed in is null, returning without adding map.");
            return;
        }
        Logger.v(TAG, "Adding new map: %s", map);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            add(key, value);
        }
    }

    @Override
    public void addMap(@NonNull Map map, @NonNull Boolean base64_encoded, @Nullable String type_encoded, @Nullable String type_no_encoded) {
        if (map == null) {
            Logger.v(TAG, "Map passed in is null, returning nothing.");
            return;
        }

        String mapString = Util.mapToJSONObject(map).toString();
        Logger.v(TAG, "Adding new map: %s", map);

        if (base64_encoded) { // base64 encoded data
            add(type_encoded, Util.base64Encode(mapString));
        } else { // add it as a child node
            add(type_no_encoded, mapString);
        }
    }

    @NonNull
    public HashMap<String,Object> getMap() {
        return payload;
    }

    @NonNull
    public String toString() {
        return Util.mapToJSONObject(payload).toString();
    }

    public long getByteSize() {
        return Util.getUTF8Length(toString());
    }
}
