package com.snowplowanalytics.snowplow.tracker;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.emitter.EventStore;
import com.snowplowanalytics.snowplow.emitter.EmitterEvent;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;
import com.snowplowanalytics.snowplow.payload.Payload;
import com.snowplowanalytics.snowplow.payload.TrackerPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockEventStore implements EventStore {
    public HashMap<Long, Payload> db = new HashMap<>();
    public long lastInsertedRow = -1;

    @Override
    public void add(@NonNull Payload payload) {
        synchronized (this) {
            lastInsertedRow++;
            Logger.v("MockEventStore", "Add %s", payload);
            db.put(lastInsertedRow, payload);
        }
    }

    @Override
    public boolean removeEvent(long id) {
        synchronized (this) {
            Logger.v("MockEventStore", "Remove %s", id);
            return db.remove(id) != null;
        }
    }

    @Override
    public boolean removeEvents(@NonNull List<Long> ids) {
        boolean result = true;
        for (long id : ids) {
            boolean removed = removeEvent(id);
            result = result && removed;
        }
        return result;
    }

    @Override
    public boolean removeAllEvents() {
        synchronized (this) {
            Logger.v("MockEventStore", "Remove all");
            db = new HashMap<>();
            lastInsertedRow = 0;
        }
        return true;
    }

    @Override
    public long getSize() {
        return db.size();
    }

    @NonNull
    @Override
    public List<EmitterEvent> getEmittableEvents(int queryLimit) {
        synchronized (this) {
            List<Long> eventIds = new ArrayList<>();
            List<String> eventPayloads = new ArrayList<>();
            List<EmitterEvent> events = new ArrayList<>();
            for (Map.Entry<Long, Payload> entry : db.entrySet()) {
                Payload payloadCopy = new TrackerPayload();
                payloadCopy.addMap(entry.getValue().getMap());
                EmitterEvent event = new EmitterEvent(payloadCopy, entry.getKey());
                eventIds.add(event.eventId);
                eventPayloads.add(payloadCopy.getMap().toString());
                events.add(event);
            }
            if (queryLimit < events.size()) {
                events = events.subList(0, queryLimit);
            }
            Logger.v("MockEventStore", "getEmittableEvents ids: %s", eventIds);
            Logger.v("MockEventStore", "getEmittableEvents payloads: %s", eventPayloads);
            return events;
        }
    }
}
