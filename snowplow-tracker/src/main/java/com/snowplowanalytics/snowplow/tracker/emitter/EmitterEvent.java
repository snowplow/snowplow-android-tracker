package com.snowplowanalytics.snowplow.tracker.emitter;

import android.support.annotation.NonNull;

import com.snowplowanalytics.snowplow.tracker.payload.Payload;

public class EmitterEvent {

    public final Payload payload;
    public final long eventId;

    public EmitterEvent(@NonNull Payload payload, long eventId) {
        this.payload = payload;
        this.eventId = eventId;
    }

    @Override
    public String toString() {
        return "EmitterEvent{" +
                "payload=" + payload +
                ", eventId=" + eventId +
                '}';
    }
}
