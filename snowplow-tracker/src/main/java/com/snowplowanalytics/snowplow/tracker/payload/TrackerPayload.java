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

package com.snowplowanalytics.snowplow.tracker.payload;

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
    public void add(String key, String value) {
        if (value == null || value.isEmpty()) {
            Logger.v(TAG, "The keys value is empty, returning without adding key: %s", key);
            return;
        }
        Logger.v(TAG, "Adding new kv pair: " + key + "->" + value);
        payload.put(key, value);
    }

    @Override
    public void add(String key, Object value) {
        if (value == null) {
            Logger.v(TAG, "The keys value is empty, returning without adding key: %s", key);
            return;
        }
        Logger.v(TAG, "Adding new kv pair: " + key + "->" + value);
        payload.put(key, value);
    }

    @Override
    public void addMap(Map<String, Object> map) {
        if (map == null) {
            Logger.v(TAG, "Map passed in is null, returning without adding map.");
            return;
        }
        Logger.v(TAG, "Adding new map: %s", map);
        payload.putAll(map);
    }

    @Override
    public void addMap(Map map, Boolean base64_encoded, String type_encoded, String type_no_encoded) {
        // Return if we don't have a map
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

    public Map getMap() {
        return payload;
    }

    public String toString() {
        return Util.mapToJSONObject(payload).toString();
    }

    public long getByteSize() {
        return Util.getUTF8Length(toString());
    }
}
