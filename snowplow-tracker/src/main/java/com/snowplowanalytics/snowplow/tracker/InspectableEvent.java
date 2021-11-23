package com.snowplowanalytics.snowplow.tracker;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.tracker.StateFuture;
import com.snowplowanalytics.snowplow.internal.tracker.TrackerStateSnapshot;

import java.util.Map;

public interface InspectableEvent {

    @Nullable
    String getSchema();

    @Nullable
    String getName();

    @NonNull
    Map<String, Object> getPayload();

    @NonNull
    TrackerStateSnapshot getState();

    boolean addPayloadValues(@NonNull Map<String, Object> payload);
}
