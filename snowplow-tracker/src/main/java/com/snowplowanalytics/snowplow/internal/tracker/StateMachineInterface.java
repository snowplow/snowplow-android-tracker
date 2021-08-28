package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.event.Event;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.InspectableEvent;

import java.util.List;

public interface StateMachineInterface {

    @NonNull
    List<String> subscribedEventSchemasForTransitions();

    @NonNull
    List<String> subscribedEventSchemasForEntitiesGeneration();

    @Nullable
    State transition(@NonNull Event event, @Nullable State currentState);

    @NonNull
    List<SelfDescribingJson> entities(@NonNull InspectableEvent event, @Nullable State state);
}
