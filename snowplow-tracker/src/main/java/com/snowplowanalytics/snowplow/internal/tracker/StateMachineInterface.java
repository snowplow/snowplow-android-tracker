package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.event.Event;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.InspectableEvent;

import java.util.List;
import java.util.Map;

public interface StateMachineInterface {

    @NonNull
    List<String> subscribedEventSchemasForTransitions();

    @NonNull
    List<String> subscribedEventSchemasForEntitiesGeneration();

    @NonNull
    List<String> subscribedEventSchemasForPayloadUpdating();

    @Nullable
    State transition(@NonNull Event event, @Nullable State state);

    @Nullable
    List<SelfDescribingJson> entities(@NonNull InspectableEvent event, @Nullable State state);

    @Nullable
    Map<String, Object> payloadValues(@NonNull InspectableEvent event, @Nullable State state);
}
