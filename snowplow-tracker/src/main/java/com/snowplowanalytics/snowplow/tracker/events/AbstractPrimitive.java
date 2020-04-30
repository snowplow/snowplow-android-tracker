package com.snowplowanalytics.snowplow.tracker.events;

import android.support.annotation.NonNull;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;

public abstract class AbstractPrimitive extends AbstractEvent {

    AbstractPrimitive(Builder<?> builder) {
        super(builder);
    }

    /**
     * @deprecated As of release 1.4.2, it will be removed in the version 2.0.0.
     * replaceable by use of {@link #getDataPayload()} and {@link #getName()}.
     *
     * @return the event payload
     */
    @Override
    @Deprecated
    public @NonNull TrackerPayload getPayload() {
        TrackerPayload payload = new TrackerPayload();
        payload.add(Parameters.EVENT, getName());
        payload.addMap(getDataPayload());
        return putDefaultParams(payload);
    }

    /**
     * @return The name of the event.
     */
    public abstract @NonNull String getName();
}
