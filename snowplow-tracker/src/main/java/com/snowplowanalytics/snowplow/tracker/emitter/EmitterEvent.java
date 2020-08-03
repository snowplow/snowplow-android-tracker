package com.snowplowanalytics.snowplow.tracker.emitter;

import android.support.annotation.NonNull;

import com.snowplowanalytics.snowplow.tracker.payload.Payload;

public class EmitterEvent {

    public final Payload payload;
    public final long eventId;

    public EmitterEvent(@NonNull Payload payload, @NonNull long eventId) {
        this.payload = payload;
        this.eventId = eventId;
    }
}
