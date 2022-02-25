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

package com.snowplowanalytics.snowplow.payload;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.utils.Preconditions;
import com.snowplowanalytics.snowplow.internal.utils.Util;
import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext;

import org.json.JSONObject;

/**
 * Returns a SelfDescribingJson object which will contain
 * both the Schema and Data.
 */
public class SelfDescribingJson {

    private final String TAG = SelfDescribingJson.class.getSimpleName();
    private final HashMap<String,Object> payload = new HashMap<>();

    /**
     * Builds a SelfDescribingJson object
     *
     * @param schema the schema string
     */
    public SelfDescribingJson(@NonNull String schema) {
        this(schema, new HashMap<>());
    }

    /**
     * Builds a SelfDescribingJson object
     *
     * @param schema the schema string
     * @param data to nest into the object
     *        as a TrackerPayload
     */
    public SelfDescribingJson(@NonNull String schema, @NonNull TrackerPayload data) {
        setSchema(schema);
        setData(data);
    }

    /**
     * Builds a SelfDescribingJson object
     *
     * @param schema the schema string
     * @param data to nest into the object
     *        as a SelfDescribingJson
     */
    public SelfDescribingJson(@NonNull String schema, @NonNull SelfDescribingJson data) {
        setSchema(schema);
        setData(data);
    }

    /**
     * Builds a SelfDescribingJson object
     *
     * @param schema the schema string
     * @param data to nest into the object
     *        as a POJO
     */
    public SelfDescribingJson(@NonNull String schema, @NonNull Object data) {
        setSchema(schema);
        setData(data);
    }

    /**
     * Sets the Schema for the SelfDescribingJson
     *
     * @param schema a valid schema string
     * @return itself if it passes precondition
     *         checks
     */
    @NonNull
    public SelfDescribingJson setSchema(@NonNull String schema) {
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
     * @return itself
     */
    @NonNull
    public SelfDescribingJson setData(@Nullable TrackerPayload trackerPayload) {
        if (trackerPayload == null) {
            return this;
        }
        payload.put(Parameters.DATA, trackerPayload.getMap());
        return this;
    }

    /**
     * Adds data to the SelfDescribingJson
     * - Accepts a POJO
     *
     * @param data the data to be added to the SelfDescribingJson
     * @return itself
     */
    @NonNull
    public SelfDescribingJson setData(@Nullable Object data) {
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
     * @return itself
     */
    @NonNull
    public SelfDescribingJson setData(@Nullable SelfDescribingJson selfDescribingJson) {
        if (selfDescribingJson == null) {
            return this;
        }
        payload.put(Parameters.DATA, selfDescribingJson.getMap());
        return this;
    }

    @NonNull
    public Map<String, Object> getMap() {
        return payload;
    }

    @NonNull
    public String toString() {
        return new JSONObject(payload).toString();
    }

    public long getByteSize() {
        return Util.getUTF8Length(toString());
    }
}
