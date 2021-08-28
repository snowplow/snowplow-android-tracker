package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snowplowanalytics.snowplow.event.Event;
import com.snowplowanalytics.snowplow.event.ScreenView;
import com.snowplowanalytics.snowplow.event.SelfDescribing;
import com.snowplowanalytics.snowplow.event.Timing;
import com.snowplowanalytics.snowplow.internal.emitter.Emitter;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.InspectableEvent;
import com.snowplowanalytics.snowplow.tracker.LogLevel;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

import android.content.Context;

@RunWith(AndroidJUnit4.class)
public class StateManagerTest {

    @Test
    public void testStateManager() {
        StateManager stateManager = new StateManager();
        stateManager.addStateMachine(new MockStateMachine(), "identifier");

        SelfDescribing eventInc = new SelfDescribing("inc", new HashMap() {{ put("value", 1); }});
        SelfDescribing eventDec = new SelfDescribing("dec", new HashMap() {{ put("value", 2); }});
        SelfDescribing event = new SelfDescribing("nothing", new HashMap() {{ put("value", 3); }});

        Map<String, StateFuture> state = stateManager.trackerStateByProcessedEvent(eventInc);
        MockState mockState = (MockState)(state.get("identifier").getState());
        assertEquals(1, mockState.value);
        InspectableEvent e = new TrackerEvent(eventInc, state);
        List<SelfDescribingJson> entities = stateManager.entitiesByProcessedEvent(e);
        Map<String,Integer> data = (Map<String, Integer>) entities.get(0).getMap().get("data");
        assertEquals(1, data.get("value").intValue());

        state = stateManager.trackerStateByProcessedEvent(eventInc);
        mockState = (MockState)(state.get("identifier").getState());
        assertEquals(2, mockState.value);
        e = new TrackerEvent(eventInc, state);
        entities = stateManager.entitiesByProcessedEvent(e);
        data = (Map<String, Integer>) entities.get(0).getMap().get("data");
        assertEquals(2, data.get("value").intValue());

        state = stateManager.trackerStateByProcessedEvent(eventDec);
        mockState = (MockState)(state.get("identifier").getState());
        assertEquals(1, mockState.value);
        e = new TrackerEvent(eventDec, state);
        entities = stateManager.entitiesByProcessedEvent(e);
        data = (Map<String, Integer>) entities.get(0).getMap().get("data");
        assertEquals(1, data.get("value").intValue());

        state = stateManager.trackerStateByProcessedEvent(event);
        mockState = (MockState)(state.get("identifier").getState());
        assertEquals(1, mockState.value);
        e = new TrackerEvent(event, state);
        entities = stateManager.entitiesByProcessedEvent(e);
        data = (Map<String, Integer>) entities.get(0).getMap().get("data");
        assertEquals(1, data.get("value").intValue());
    }

    @Test
    public void testAddRemoveStateMachine() {
        StateManager stateManager = new StateManager();
        stateManager.addStateMachine(new MockStateMachine(), "identifier");
        stateManager.removeStateMachine("identifier");

        SelfDescribing eventInc = new SelfDescribing("inc", new HashMap() {{ put("value", 1); }});

        Map<String, StateFuture> state = stateManager.trackerStateByProcessedEvent(eventInc);
        StateFuture stateFuture = state.get("identifier");
        assertNull(stateFuture);
        InspectableEvent e = new TrackerEvent(eventInc, state);
        List<SelfDescribingJson> entities = stateManager.entitiesByProcessedEvent(e);
        assertEquals(0, entities.size());
    }

    @Test
    public void testScreenState() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Emitter emitter = new Emitter.EmitterBuilder("http://snowplow-fake-url.com", context).build();
        Tracker tracker = new Tracker.TrackerBuilder(emitter, "namespace", "appId", context)
                .screenContext(true)
                .base64(false)
                .level(LogLevel.VERBOSE)
                .build();

        // Send events
        tracker.track(new Timing("category", "variable", 123));

        tracker.track(new ScreenView("screen1"));

        tracker.track(new Timing("category", "variable", 123));

        tracker.track(new ScreenView("screen2"));

        tracker.track(new Timing("category", "variable", 123));
    }
}

// Mock classes

class MockState implements State {
    int value;

    MockState(int value) {
        this.value = value;
    }
}

class MockStateMachine implements StateMachineInterface {

    @NonNull
    @Override
    public List<String> subscribedEventSchemasForTransitions() {
        return new LinkedList<>(Arrays.asList("inc", "dec"));
    }

    @NonNull
    @Override
    public List<String> subscribedEventSchemasForEntitiesGeneration() {
        return new LinkedList<>(Collections.singletonList("*"));
    }

    @Nullable
    @Override
    public State transition(@NonNull Event event, @Nullable State currentState) {
        SelfDescribing e = (SelfDescribing)event;
        MockState state = (MockState)currentState;
        if (state == null) {
            state = new MockState(0);
        }
        if (e.getSchema().equals("inc")) {
            return new MockState(state.value+1);
        } else if (e.getSchema().equals("dec")) {
            return new MockState(state.value-1);
        } else {
            return new MockState(0);
        }
    }

    @NonNull
    @Override
    public List<SelfDescribingJson> entities(@NonNull InspectableEvent event, @NonNull State state) {
        MockState mockState = (MockState)state;
        SelfDescribingJson sdj = new SelfDescribingJson("enitity", new HashMap<String,Integer>() {{
            put("value", mockState.value);
        }});
        return new LinkedList<>(Arrays.asList(sdj));
    }
}
