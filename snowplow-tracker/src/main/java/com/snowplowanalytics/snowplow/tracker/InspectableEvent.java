package com.snowplowanalytics.snowplow.tracker;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.tracker.StateFuture;

import java.util.Map;

public interface InspectableEvent {

    @Nullable
    String getSchema();

    @Nullable
    String getName();

    @NonNull
    Map<String, Object> getPayload();

    @NonNull
    Map<String, StateFuture> getState();
}
