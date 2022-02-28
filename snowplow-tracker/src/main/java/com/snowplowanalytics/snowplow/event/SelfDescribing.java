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

package com.snowplowanalytics.snowplow.event;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.internal.utils.Preconditions;

import java.util.Map;

/**
 * A SelfDescribing event.
 */
public class SelfDescribing extends AbstractSelfDescribing {

    /**
     * The properties of the event. Has two field:
     * * a "data" field containing the event properties,
     * * a "schema" field identifying the schema against which the data is validated.
     */
    @NonNull
    public final SelfDescribingJson eventData;

    /** A "data" field containing the event properties. */
    @NonNull
    private final Map<String, Object> payload;
    /** A "schema" field identifying the schema against which the data is validated. */
    @NonNull
    private final String schema;

    /**
     * Creates a SelfDescribing event.
     * @param eventData The properties of the event. Has two field: a "data" field containing the event
     *                  properties and a "schema" field identifying the schema against which the data is validated.
     */
    public SelfDescribing(@NonNull SelfDescribingJson eventData) {
        Preconditions.checkNotNull(eventData);
        Map<String, Object> eventDataMap = eventData.getMap();
        Preconditions.checkNotNull(eventDataMap);
        Map<String, Object> payload = (Map<String, Object>)eventDataMap.get(Parameters.DATA);
        Preconditions.checkNotNull(payload);
        this.payload = payload;
        String schema = (String)eventDataMap.get(Parameters.SCHEMA);
        Preconditions.checkNotNull(schema);
        this.schema = schema;
        this.eventData = eventData;
    }

    /**
     * Creates a SelfDescribing event.
     * @param schema The schema against which the payload is validated.
     * @param payload The event properties.
     */
    public SelfDescribing(@NonNull String schema, @NonNull Map<String, Object> payload) {
        this.schema = schema;
        this.payload = payload;
        this.eventData = new SelfDescribingJson(schema, payload);
    }

    // Tracker methods

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        return payload;
    }

    @Override
    public @NonNull String getSchema() {
        return schema;
    }
}
