package com.snowplowanalytics.snowplow.tracker;

import android.support.annotation.NonNull;

import com.snowplowanalytics.snowplow.tracker.events.AbstractPrimitive;
import com.snowplowanalytics.snowplow.tracker.events.AbstractSelfDescribing;
import com.snowplowanalytics.snowplow.tracker.events.Event;
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

    private TrackerEvent(Event event) {
        super();
        eventId = UUID.fromString(event.getEventId());
        contexts = event.getContexts();
        timestamp = event.getDeviceCreatedTimestamp();
        trueTimestamp = event.getTrueTimestamp();
        payload = event.getDataPayload();
        isService = event instanceof TrackerError;
    }

    static @NonNull TrackerEvent createWithPrimitive(AbstractPrimitive event) {
        TrackerEvent trackerEvent = new TrackerEvent(event);
        trackerEvent.eventName = event.getName();
        trackerEvent.isPrimitive = true;
        return trackerEvent;
    }

    static @NonNull TrackerEvent createWithSelfDescribing(AbstractSelfDescribing event) {
        TrackerEvent trackerEvent = new TrackerEvent(event);
        trackerEvent.schema = event.getSchema();
        trackerEvent.isPrimitive = false;
        return trackerEvent;
    }
}
