package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.event.Event;
import com.snowplowanalytics.snowplow.event.ScreenView;
import com.snowplowanalytics.snowplow.event.SelfDescribing;
import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.InspectableEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScreenStateMachine implements StateMachineInterface {

    @NonNull
    @Override
    public List<String> subscribedEventSchemasForTransitions() {
        return Collections.singletonList(TrackerConstants.SCHEMA_SCREEN_VIEW);
    }

    @NonNull
    @Override
    public List<String> subscribedEventSchemasForEntitiesGeneration() {
        return Collections.singletonList("*");
    }

    @NonNull
    @Override
    public List<String> subscribedEventSchemasForPayloadUpdating() {
        return Collections.singletonList(TrackerConstants.SCHEMA_SCREEN_VIEW);
    }

    @Nullable
    @Override
    public State transition(@NonNull Event event, @Nullable State currentState) {
        ScreenView screenView = (ScreenView) event;
        ScreenState screenState;
        if (currentState != null) {
            screenState = (ScreenState) currentState;
        } else {
            screenState = new ScreenState();
        }
        screenView.updateScreenState(screenState);
        return screenState;
    }

    @NonNull
    @Override
    public List<SelfDescribingJson> entities(@NonNull InspectableEvent event, @Nullable State state) {
        if (state == null) return new ArrayList<>();
        ScreenState screenState = (ScreenState) state;
        SelfDescribingJson entity = screenState.getCurrentScreen(true);
        return Collections.singletonList(entity);
    }

    @Nullable
    @Override
    public Map<String, Object> payloadValues(@NonNull InspectableEvent event, @Nullable State state) {
        if (state instanceof ScreenState) {
            ScreenState screenState = ((ScreenState) state);
            Map<String, Object> addedValues = new HashMap<>();
            addedValues.put(Parameters.SV_PREVIOUS_NAME, screenState.getPreviousName());
            addedValues.put(Parameters.SV_PREVIOUS_TYPE, screenState.getPreviousType());
            addedValues.put(Parameters.SV_PREVIOUS_ID, screenState.getPreviousId());
            return addedValues;
        }
        return null;
    }
}
