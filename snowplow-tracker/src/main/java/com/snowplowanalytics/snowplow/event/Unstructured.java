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

package com.snowplowanalytics.snowplow.event;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.internal.utils.Preconditions;

import java.util.Map;

/**
 * Constructs an SelfDescribing event object.
 */
public class Unstructured extends AbstractSelfDescribing {

    @NonNull
    public final SelfDescribingJson eventData;

    @NonNull
    private final Map<String, Object> payload;
    @NonNull
    private final String schema;

    public Unstructured(@NonNull SelfDescribingJson eventData) {
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

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        return payload;
    }

    @Override
    public @NonNull String getSchema() {
        return schema;
    }
}
