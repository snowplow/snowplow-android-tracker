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

package com.snowplowanalytics.snowplow.tracker;

import android.support.annotation.NonNull;

import com.snowplowanalytics.snowplow.tracker.events.AbstractPrimitive;
import com.snowplowanalytics.snowplow.tracker.events.AbstractSelfDescribing;
import com.snowplowanalytics.snowplow.tracker.events.Event;
import com.snowplowanalytics.snowplow.tracker.events.TrackerError;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.utils.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class TrackerEvent {
    Map<String, Object> payload;
    String schema;
    String eventName;
    UUID eventId;
    long timestamp;
    Long trueTimestamp;
    List<SelfDescribingJson> contexts;

    boolean isPrimitive;
    boolean isService;

    TrackerEvent(@NonNull Event event) {
        String userEventId = event.getActualEventId();
        String newEventId = userEventId != null ? userEventId : Util.getUUIDString();
        eventId = UUID.fromString(newEventId);

        Long userTimestamp = event.getActualDeviceCreatedTimestamp();
        timestamp = userTimestamp != null ? userTimestamp : System.currentTimeMillis();

        contexts = new ArrayList<>(event.getContexts());
        trueTimestamp = event.getTrueTimestamp();
        payload = new HashMap<>(event.getDataPayload());

        isService = event instanceof TrackerError;
        if (event instanceof AbstractPrimitive) {
            eventName = ((AbstractPrimitive) event).getName();
            isPrimitive = true;
        } else {
            schema = ((AbstractSelfDescribing) event).getSchema();
            isPrimitive = false;
        }
    }
}
