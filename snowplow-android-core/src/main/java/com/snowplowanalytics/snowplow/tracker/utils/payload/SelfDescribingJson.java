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

package com.snowplowanalytics.snowplow.tracker.utils.payload;

import java.util.HashMap;
import java.util.Map;
import java.nio.charset.Charset;

import com.snowplowanalytics.snowplow.tracker.Payload;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.utils.Preconditions;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.Util;

public class SelfDescribingJson implements Payload {

    private final String TAG = SelfDescribingJson.class.getSimpleName();
    private final HashMap<String,Object> payload = new HashMap<String,Object>();
    
    public SelfDescribingJson(String schema) {
        this(schema, new HashMap<>());
    }

    public SelfDescribingJson(String schema, TrackerPayload data) {
        setSchema(schema);
        setData(data);
    }

    public SelfDescribingJson(String schema, SelfDescribingJson data) {
        setSchema(schema);
        setData(data);
    }

    public SelfDescribingJson(String schema, Object data) {
        setSchema(schema);
        setData(data);
    }

    /**
     * Sets the Schema for the SelfDescribingJson
     *
     * @param schema a valid schema string
     */
    public SelfDescribingJson setSchema(String schema) {
        Preconditions.checkNotNull(schema, "schema cannot be null");
        Preconditions.checkArgument(!schema.isEmpty(), "schema cannot be empty.");
        payload.put(Parameters.SCHEMA, schema);
        return this;
    }

    /**
     * Adds data to the SelfDescribingJson
     * - Accepts a TrackerPayload object
     *
     * @param trackerPayload the data to be added to the SelfDescribingJson
     */
    public SelfDescribingJson setData(TrackerPayload trackerPayload) {
        if (trackerPayload == null) {
            return this;
        }
        payload.put(Parameters.DATA, trackerPayload.getMap());
        return this;
    }

    /**
     * Adds data to the SelfDescribingJson
     *
     * @param data the data to be added to the SelfDescribingJson
     */
    public SelfDescribingJson setData(Object data) {
        if (data == null) {
            return this;
        }
        payload.put(Parameters.DATA, data);
        return this;
    }

    /**
     * Allows us to add data from one SelfDescribingJson into another
     * without copying over the Schema.
     *
     * @param selfDescribingJson the payload to add to the SelfDescribingJson
     */
    public SelfDescribingJson setData(SelfDescribingJson selfDescribingJson) {
        if (payload == null) {
            return this;
        }
        payload.put(Parameters.DATA, selfDescribingJson.getMap());
        return this;
    }

    @Deprecated
    @Override
    public void add(String key, String value) {
        /*
         * We intentionally do nothing because we do not want our SchemaPayload
         * to do anything except accept a 'data' and 'schema'
         */
        Logger.v(TAG, "Payload: add(String, String) method called - Doing nothing.");
    }

    @Deprecated
    @Override
    public void add(String key, Object value) {
        /*
         * We intentionally do nothing because we do not want our SchemaPayload
         * to do anything except accept a 'data' and 'schema'
         */
        Logger.v(TAG, "Payload: add(String, Object) method called - Doing nothing.");
    }

    @Deprecated
    @Override
    public void addMap(Map<String, Object> map) {
        /*
         * We intentionally do nothing because we do not want our SchemaPayload
         * to do anything except accept a 'data' and 'schema'
         */
        Logger.v(TAG, "Payload: addMap(Map<String, Object>) method called - Doing nothing.");
    }

    @Deprecated
    @Override
    public void addMap(Map map, Boolean base64_encoded, String type_encoded,
                       String type_no_encoded) {
        /*
         * We intentionally do nothing because we do not want our SchemaPayload
         * to do anything except accept a 'data' and 'schema'
         */
        Logger.v(TAG, "Payload: addMap(Map, Boolean, String, String) method called - Doing nothing.");
    }

    public Map<String, Object> getMap() {
        return payload;
    }

    public String toString() {
        return Util.mapToJSONObject(payload).toString();
    }

    public long getByteSize() {
        return toString().getBytes(Charset.forName("UTF-8")).length;
    }
}
