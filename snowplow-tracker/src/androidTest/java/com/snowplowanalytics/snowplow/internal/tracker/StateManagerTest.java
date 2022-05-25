package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snowplowanalytics.snowplow.event.Background;
import com.snowplowanalytics.snowplow.event.DeepLinkReceived;
import com.snowplowanalytics.snowplow.event.Event;
import com.snowplowanalytics.snowplow.event.Foreground;
import com.snowplowanalytics.snowplow.event.ScreenView;
import com.snowplowanalytics.snowplow.event.SelfDescribing;
import com.snowplowanalytics.snowplow.event.Timing;
import com.snowplowanalytics.snowplow.internal.emitter.Emitter;
import com.snowplowanalytics.snowplow.payload.Payload;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.InspectableEvent;
import com.snowplowanalytics.snowplow.tracker.LogLevel;
import com.snowplowanalytics.snowplow.tracker.MockEventStore;

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
        stateManager.addOrReplaceStateMachine(new MockStateMachine(), "identifier");

        SelfDescribing eventInc = new SelfDescribing("inc", new HashMap() {{ put("value", 1); }});
        SelfDescribing eventDec = new SelfDescribing("dec", new HashMap() {{ put("value", 2); }});
        SelfDescribing event = new SelfDescribing("event", new HashMap() {{ put("value", 3); }});

        TrackerStateSnapshot trackerState = stateManager.trackerStateForProcessedEvent(eventInc);
        MockState mockState = (MockState)(trackerState.getState("identifier"));
        assertEquals(1, mockState.value);
        InspectableEvent e = new TrackerEvent(eventInc, trackerState);
        List<SelfDescribingJson> entities = stateManager.entitiesForProcessedEvent(e);
        Map<String,Integer> data = (Map<String, Integer>) entities.get(0).getMap().get("data");
        assertEquals(1, data.get("value").intValue());
        assertTrue(stateManager.addPayloadValuesToEvent(e));
        assertNull(e.getPayload().get("newParam"));

        trackerState = stateManager.trackerStateForProcessedEvent(eventInc);
        mockState = (MockState)(trackerState.getState("identifier"));
        assertEquals(2, mockState.value);
        e = new TrackerEvent(eventInc, trackerState);
        entities = stateManager.entitiesForProcessedEvent(e);
        data = (Map<String, Integer>) entities.get(0).getMap().get("data");
        assertEquals(2, data.get("value").intValue());
        assertTrue(stateManager.addPayloadValuesToEvent(e));
        assertNull(e.getPayload().get("newParam"));

        trackerState = stateManager.trackerStateForProcessedEvent(eventDec);
        mockState = (MockState)(trackerState.getState("identifier"));
        assertEquals(1, mockState.value);
        e = new TrackerEvent(eventDec, trackerState);
        entities = stateManager.entitiesForProcessedEvent(e);
        data = (Map<String, Integer>) entities.get(0).getMap().get("data");
        assertEquals(1, data.get("value").intValue());
        assertTrue(stateManager.addPayloadValuesToEvent(e));
        assertNull(e.getPayload().get("newParam"));

        trackerState = stateManager.trackerStateForProcessedEvent(event);
        mockState = (MockState)(trackerState.getState("identifier"));
        assertEquals(1, mockState.value);
        e = new TrackerEvent(event, trackerState);
        entities = stateManager.entitiesForProcessedEvent(e);
        data = (Map<String, Integer>) entities.get(0).getMap().get("data");
        assertEquals(1, data.get("value").intValue());
        assertTrue(stateManager.addPayloadValuesToEvent(e));
        assertEquals("value", e.getPayload().get("newParam"));
    }

    @Test
    public void testAddRemoveStateMachine() {
        StateManager stateManager = new StateManager();
        stateManager.addOrReplaceStateMachine(new MockStateMachine(), "identifier");
        stateManager.removeStateMachine("identifier");

        SelfDescribing eventInc = new SelfDescribing("inc", new HashMap() {{ put("value", 1); }});

        TrackerStateSnapshot trackerState = stateManager.trackerStateForProcessedEvent(eventInc);
        State state = trackerState.getState("identifier");
        assertNull(state);
        InspectableEvent e = new TrackerEvent(eventInc, trackerState);
        List<SelfDescribingJson> entities = stateManager.entitiesForProcessedEvent(e);
        assertEquals(0, entities.size());
    }

    @Test
    public void testScreenStateMachine() throws InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MockEventStore eventStore = new MockEventStore();
        Emitter emitter = new Emitter(context, "http://snowplow-fake-url.com", new Emitter.EmitterBuilder()
                .eventStore(eventStore)
        );
        Tracker tracker = new Tracker(new Tracker.TrackerBuilder(emitter, "namespace", "appId", context)
                .screenContext(true)
                .base64(false)
                .level(LogLevel.VERBOSE)
        );

        // Send events
        tracker.track(new Timing("category", "variable", 123));
        Thread.sleep(1000);
        if (eventStore.lastInsertedRow == -1) fail();
        Payload payload = eventStore.db.get(eventStore.lastInsertedRow);
        eventStore.removeAllEvents();
        String entities = (String) payload.getMap().get("co");
        assertNull(entities);

        tracker.track(new ScreenView("screen1"));
        Thread.sleep(1000);
        if (eventStore.lastInsertedRow == -1) fail();
        payload = eventStore.db.get(eventStore.lastInsertedRow);
        eventStore.removeAllEvents();
        entities = (String) payload.getMap().get("co");
        assertNotNull(entities);
        assertTrue(entities.contains("screen1"));
        assertEquals(1, entities.split("screen1", -1).length - 1);

        tracker.track(new Timing("category", "variable", 123));
        Thread.sleep(1000);
        if (eventStore.lastInsertedRow == -1) fail();
        payload = eventStore.db.get(eventStore.lastInsertedRow);
        eventStore.removeAllEvents();
        entities = (String) payload.getMap().get("co");
        assertTrue(entities.contains("screen1"));
        assertEquals(1, entities.split("screen1", -1).length - 1);

        tracker.track(new ScreenView("screen2"));
        Thread.sleep(1000);
        if (eventStore.lastInsertedRow == -1) fail();
        payload = eventStore.db.get(eventStore.lastInsertedRow);
        eventStore.removeAllEvents();
        entities = (String) payload.getMap().get("co");
        assertTrue(entities.contains("screen2"));
        assertEquals(1, entities.split("screen2", -1).length - 1);
        String eventPayload = (String) payload.getMap().get("ue_pr");
        assertTrue(eventPayload.contains("screen1"));
        assertTrue(eventPayload.contains("screen2"));

        tracker.track(new Timing("category", "variable", 123));
        Thread.sleep(1000);
        if (eventStore.lastInsertedRow == -1) fail();
        payload = eventStore.db.get(eventStore.lastInsertedRow);
        eventStore.removeAllEvents();
        entities = (String) payload.getMap().get("co");
        assertTrue(entities.contains("screen2"));
        assertEquals(1, entities.split("screen2", -1).length - 1);

        tracker.track(new Timing("category", "variable", 123));
        Thread.sleep(1000);
        if (eventStore.lastInsertedRow == -1) fail();
        payload = eventStore.db.get(eventStore.lastInsertedRow);
        eventStore.removeAllEvents();
        entities = (String) payload.getMap().get("co");
        assertTrue(entities.contains("screen2"));
        assertEquals(1, entities.split("screen2", -1).length - 1);
    }

    @Test
    public void testLifecycleStateMachine() throws InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MockEventStore eventStore = new MockEventStore();
        Emitter emitter = new Emitter(context, "http://snowplow-fake-url.com", new Emitter.EmitterBuilder()
                .eventStore(eventStore)
        );
        Tracker tracker = new Tracker(new Tracker.TrackerBuilder(emitter, "namespace", "appId", context)
                .base64(false)
                .level(LogLevel.VERBOSE)
                .lifecycleEvents(true)
        );

        // Send events
        tracker.track(new Timing("category", "variable", 123));
        Thread.sleep(1000);
        if (eventStore.lastInsertedRow == -1) fail();
        Payload payload = eventStore.db.get(eventStore.lastInsertedRow);
        eventStore.removeAllEvents();
        String entities = (String) payload.getMap().get("co");
        assertNotNull(entities);
        assertTrue(entities.contains("\"isVisible\":true"));
        assertEquals(1, entities.split("isVisible", -1).length - 1);

        tracker.track(new Background());
        Thread.sleep(1000);
        if (eventStore.lastInsertedRow == -1) fail();
        payload = eventStore.db.get(eventStore.lastInsertedRow);
        eventStore.removeAllEvents();
        entities = (String) payload.getMap().get("co");
        assertNotNull(entities);
        assertTrue(entities.contains("\"isVisible\":false"));
        assertEquals(1, entities.split("isVisible", -1).length - 1);

        tracker.track(new ScreenView("screen1"));
        Thread.sleep(1000);
        if (eventStore.lastInsertedRow == -1) fail();
        payload = eventStore.db.get(eventStore.lastInsertedRow);
        eventStore.removeAllEvents();
        entities = (String) payload.getMap().get("co");
        assertNotNull(entities);
        System.out.println(entities);
        assertTrue(entities.contains("\"isVisible\":false"));
        assertEquals(1, entities.split("isVisible", -1).length - 1);

        tracker.track(new Foreground().foregroundIndex(9));
        Thread.sleep(1000);
        if (eventStore.lastInsertedRow == -1) fail();
        payload = eventStore.db.get(eventStore.lastInsertedRow);
        eventStore.removeAllEvents();
        entities = (String) payload.getMap().get("co");
        assertNotNull(entities);
        System.out.println(entities);
        assertTrue(entities.contains("\"isVisible\":true"));
        assertTrue(entities.contains("\"index\":9"));
        assertEquals(1, entities.split("isVisible", -1).length - 1);

        tracker.track(new ScreenView("screen1"));
        Thread.sleep(1000);
        if (eventStore.lastInsertedRow == -1) fail();
        payload = eventStore.db.get(eventStore.lastInsertedRow);
        eventStore.removeAllEvents();
        entities = (String) payload.getMap().get("co");
        assertNotNull(entities);
        System.out.println(entities);
        assertTrue(entities.contains("\"isVisible\":true"));
        assertTrue(entities.contains("\"index\":9"));
        assertEquals(1, entities.split("isVisible", -1).length - 1);
    }

    @Test
    public void testDeepLinkStateMachine() throws InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MockEventStore eventStore = new MockEventStore();
        Emitter emitter = new Emitter(context, "http://snowplow-fake-url.com", new Emitter.EmitterBuilder()
                .eventStore(eventStore)
        );
        Tracker tracker = new Tracker(new Tracker.TrackerBuilder(emitter, "namespace", "appId", context)
                .base64(false)
                .deepLinkContext(true)
        );

        // Send events
        tracker.track(new Timing("category", "variable", 123));
        Thread.sleep(1000);
        if (eventStore.lastInsertedRow == -1) fail();
        Payload payload = eventStore.db.get(eventStore.lastInsertedRow);
        eventStore.removeAllEvents();
        String entities = (String) payload.getMap().get("co");
        assertNull(entities);

        tracker.track(new DeepLinkReceived("http://www.homepage.com"));
        Thread.sleep(1000);
        if (eventStore.lastInsertedRow == -1) fail();
        payload = eventStore.db.get(eventStore.lastInsertedRow);
        eventStore.removeAllEvents();
        entities = (String) payload.getMap().get("co");
        assertNull(entities);

        tracker.track(new ScreenView("screen1"));
        Thread.sleep(1000);
        if (eventStore.lastInsertedRow == -1) fail();
        payload = eventStore.db.get(eventStore.lastInsertedRow);
        eventStore.removeAllEvents();
        entities = (String) payload.getMap().get("co");
        assertNotNull(entities);
        assertTrue(entities.contains("www.homepage.com"));
        assertEquals(1, entities.split("url", -1).length - 1);

        tracker.track(new Timing("category", "variable", 123));
        Thread.sleep(1000);
        if (eventStore.lastInsertedRow == -1) fail();
        payload = eventStore.db.get(eventStore.lastInsertedRow);
        eventStore.removeAllEvents();
        entities = (String) payload.getMap().get("co");
        assertNull(entities);

        tracker.track(new ScreenView("screen2"));
        Thread.sleep(1000);
        if (eventStore.lastInsertedRow == -1) fail();
        payload = eventStore.db.get(eventStore.lastInsertedRow);
        eventStore.removeAllEvents();
        entities = (String) payload.getMap().get("co");
        assertNull(entities);
    }

    @Test
    public void testAllowsMultipleStateMachines() throws InterruptedException {
        StateManager stateManager = new StateManager();
        stateManager.addOrReplaceStateMachine(new MockStateMachine(), "identifier1");
        stateManager.addOrReplaceStateMachine(new MockStateMachine(), "identifier2");

        SelfDescribing eventInc = new SelfDescribing("inc", new HashMap() {{ put("value", 1); }});
        TrackerStateSnapshot trackerState = stateManager.trackerStateForProcessedEvent(eventInc);

        InspectableEvent e = new TrackerEvent(eventInc, trackerState);
        List<SelfDescribingJson> entities = stateManager.entitiesForProcessedEvent(e);
        assertEquals(2, entities.size());
    }

    @Test
    public void testDoesntDuplicateStateFromStateMachinesWithSameId() throws InterruptedException {
        StateManager stateManager = new StateManager();
        stateManager.addOrReplaceStateMachine(new MockStateMachine(), "identifier");
        stateManager.addOrReplaceStateMachine(new MockStateMachine(), "identifier");

        SelfDescribing eventInc = new SelfDescribing("inc", new HashMap() {{ put("value", 1); }});
        TrackerStateSnapshot trackerState = stateManager.trackerStateForProcessedEvent(eventInc);

        InspectableEvent e = new TrackerEvent(eventInc, trackerState);
        List<SelfDescribingJson> entities = stateManager.entitiesForProcessedEvent(e);
        assertEquals(1, entities.size());
    }

    @Test
    public void testReplacingStateMachineDoesntResetTrackerState() {
        StateManager stateManager = new StateManager();

        stateManager.addOrReplaceStateMachine(new MockStateMachine(), "identifier");
        stateManager.trackerStateForProcessedEvent(new SelfDescribing("inc", new HashMap() {{ put("value", 1); }}));
        State state1 = stateManager.trackerState.getState("identifier");

        stateManager.addOrReplaceStateMachine(new MockStateMachine(), "identifier");
        State state2 = stateManager.trackerState.getState("identifier");

        assertNotNull(state1);
        assertSame(state1, state2);
    }

    @Test
    public void testReplacingStateMachineWithDifferentOneResetsTrackerState() {
        class MockStateMachine1 extends MockStateMachine {}
        class MockStateMachine2 extends MockStateMachine {}

        StateManager stateManager = new StateManager();
        stateManager.addOrReplaceStateMachine(new MockStateMachine1(), "identifier");
        stateManager.trackerStateForProcessedEvent(new SelfDescribing("inc", new HashMap() {{ put("value", 1); }}));
        State state1 = stateManager.trackerState.getState("identifier");

        stateManager.addOrReplaceStateMachine(new MockStateMachine2(), "identifier");
        State state2 = stateManager.trackerState.getState("identifier");

        assertNotNull(state1);
        assertNotSame(state1, state2);
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

    @NonNull
    @Override
    public List<String> subscribedEventSchemasForPayloadUpdating() {
        return new LinkedList<>(Collections.singletonList("event"));
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
    public List<SelfDescribingJson> entities(@NonNull InspectableEvent event, @Nullable State state) {
        MockState mockState = (MockState)state;
        SelfDescribingJson sdj = new SelfDescribingJson("enitity", new HashMap<String,Integer>() {{
            put("value", mockState.value);
        }});
        return new LinkedList<>(Arrays.asList(sdj));
    }

    @Nullable
    @Override
    public Map<String, Object> payloadValues(@NonNull InspectableEvent event, @Nullable State state) {
        return new HashMap<String, Object>() {{ put("newParam", "value"); }};
    }
}
