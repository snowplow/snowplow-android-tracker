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

package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.event.AbstractPrimitive;
import com.snowplowanalytics.snowplow.event.AbstractSelfDescribing;
import com.snowplowanalytics.snowplow.event.Event;
import com.snowplowanalytics.snowplow.event.TrackerError;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.InspectableEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TrackerEvent implements InspectableEvent {
    Map<String, Object> payload;
    String schema;
    String eventName;
    UUID eventId;
    long timestamp;
    Long trueTimestamp;
    List<SelfDescribingJson> contexts;
    TrackerStateSnapshot state;

    boolean isPrimitive;
    boolean isService;

    public TrackerEvent(@NonNull Event event) {
        this(event, null);
    }

    public TrackerEvent(@NonNull Event event, @Nullable TrackerStateSnapshot state) {
        eventId = UUID.randomUUID();
        timestamp = System.currentTimeMillis();

        contexts = new ArrayList<>(event.getContexts());
        trueTimestamp = event.getTrueTimestamp();
        payload = new HashMap<>(event.getDataPayload());
        if (state != null) {
            this.state = state;
        } else {
            this.state = new TrackerState();
        }

        isService = event instanceof TrackerError;
        if (event instanceof AbstractPrimitive) {
            eventName = ((AbstractPrimitive) event).getName();
            isPrimitive = true;
        } else {
            schema = ((AbstractSelfDescribing) event).getSchema();
            isPrimitive = false;
        }
    }

    // InspectableEvent methods

    @Nullable
    @Override
    public String getSchema() {
        return schema;
    }

    @Nullable
    @Override
    public String getName() {
        return eventName;
    }

    @NonNull
    @Override
    public Map<String, Object> getPayload() {
        return payload;
    }

    @NonNull
    @Override
    public TrackerStateSnapshot getState() {
        return state;
    }

    @Override
    public boolean addPayloadValues(@NonNull Map<String, Object> payloadAdding) {
        boolean result = true;
        for (Map.Entry<String, Object> entry : payloadAdding.entrySet()) {
            String key = entry.getKey();
            if (payload.get(key) == null) {
                payload.put(key, entry.getValue());
            } else {
                result = false;
            }
        }
        return result;
    }

}
