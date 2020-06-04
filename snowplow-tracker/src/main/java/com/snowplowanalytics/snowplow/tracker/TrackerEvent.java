package com.snowplowanalytics.snowplow.tracker;

import android.support.annotation.NonNull;

import com.snowplowanalytics.snowplow.tracker.events.AbstractPrimitive;
import com.snowplowanalytics.snowplow.tracker.events.AbstractSelfDescribing;
import com.snowplowanalytics.snowplow.tracker.events.Event;
import com.snowplowanalytics.snowplow.tracker.events.SelfDescribing;
import com.snowplowanalytics.snowplow.tracker.events.TrackerError;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;

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

    TrackerEvent(Event event) {
        super();
        eventId = UUID.fromString(event.getEventId());
        contexts = event.getContexts();
        timestamp = event.getDeviceCreatedTimestamp();
        trueTimestamp = event.getTrueTimestamp();
        payload = event.getDataPayload();

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
