package com.snowplowanalytics.snowplow.tracker.events;

import android.support.annotation.NonNull;

import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;

public abstract class AbstractSelfDescribing extends AbstractEvent {

    AbstractSelfDescribing(Builder<?> builder) {
        super(builder);
    }

    protected AbstractSelfDescribing() { super(); }

    /**
     * @deprecated As of release 1.4.2, it will be removed in the version 2.0.0.
     * replaceable by use of {@link #getDataPayload()} and {@link #getSchema()}.
     *
     * @return the event payload
     */
    @Override
    @Deprecated
    public @NonNull SelfDescribingJson getPayload() {
        return new SelfDescribingJson(getSchema(), getDataPayload());
    }

    /**
     * @return The schema of the event.
     */
    public abstract @NonNull String getSchema();
}
