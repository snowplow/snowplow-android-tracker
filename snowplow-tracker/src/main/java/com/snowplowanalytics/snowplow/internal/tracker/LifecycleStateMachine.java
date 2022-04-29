package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.entity.LifecycleEntity;
import com.snowplowanalytics.snowplow.event.Background;
import com.snowplowanalytics.snowplow.event.Event;
import com.snowplowanalytics.snowplow.event.Foreground;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.InspectableEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LifecycleStateMachine implements StateMachineInterface {

    /*
     States: Visible, NotVisible
     Events: FG (Foreground), BG (Background)
     Transitions:
      - Visible (BG) NotVisible
      - NotVisible (FG) Visible
     Entity Generation:
      - Visible, NotVisible
     */

    @NonNull
    @Override
    public List<String> subscribedEventSchemasForTransitions() {
        return Arrays.asList(Background.SCHEMA, Foreground.SCHEMA);
    }

    @NonNull
    @Override
    public List<String> subscribedEventSchemasForEntitiesGeneration() {
        return Collections.singletonList("*");
    }

    @NonNull
    @Override
    public List<String> subscribedEventSchemasForPayloadUpdating() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public State transition(@NonNull Event event, @Nullable State currentState) {
        if (event instanceof Foreground) {
            Foreground e = (Foreground) event;
            return new LifecycleState(true, e.foregroundIndex);
        }
        if (event instanceof Background) {
            Background e = (Background) event;
            return new LifecycleState(false, e.backgroundIndex);
        }
        return null;
    }

    @Nullable
    @Override
    public List<SelfDescribingJson> entities(@NonNull InspectableEvent event, @Nullable State state) {
        if (state == null) return Collections.singletonList(new LifecycleEntity(true));

        LifecycleState s = (LifecycleState) state;
        return Collections.singletonList(new LifecycleEntity(s.isForeground).index(s.index));
    }

    @Nullable
    @Override
    public Map<String, Object> payloadValues(@NonNull InspectableEvent event, @Nullable State state) {
        return null;
    }
}
