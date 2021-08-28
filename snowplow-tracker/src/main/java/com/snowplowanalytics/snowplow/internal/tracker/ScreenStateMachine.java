package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.event.Event;
import com.snowplowanalytics.snowplow.event.ScreenView;
import com.snowplowanalytics.snowplow.event.SelfDescribing;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.InspectableEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
}
