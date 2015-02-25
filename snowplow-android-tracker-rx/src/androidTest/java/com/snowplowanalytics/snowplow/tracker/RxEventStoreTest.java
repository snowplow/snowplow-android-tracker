package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

import java.util.Map;

import com.snowplowanalytics.snowplow.tracker.utils.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.utils.payload.TrackerPayload;

public class RxEventStoreTest extends AndroidTestCase {

    // Helper Methods

    private RxEventStore getEventStore() {
        RxEventStore eventStore = new RxEventStore(getContext());
        eventStore.removeAllEvents();
        return eventStore;
    }

    private SelfDescribingJson getEvent() {
        TrackerPayload trackerPayload = new TrackerPayload();
        trackerPayload.add("someKey", "someValue");
        trackerPayload.add("anotherKey", "anotherValue");

        return new SelfDescribingJson("iglu:com.snowplowanalytics.snowplow/example/jsonschema/1-0-0", trackerPayload);
    }

    // Tests

    public void testInsertPayload() {
        RxEventStore eventStore = getEventStore();
        long id = eventStore.insertEvent(getEvent());
        long lastRowId = eventStore.getLastInsertedRowId();
        Map<String, Object> event = eventStore.getEvent(id);

        assertEquals(id, lastRowId);
        assertEquals(1, eventStore.getSize());
        assertNotNull(event);
    }

    public void testEventStoreQueries() {
        RxEventStore eventStore = getEventStore();
        eventStore.insertEvent(getEvent());

        assertEquals(1, eventStore.getAllEvents().size());
        assertEquals(1, eventStore.getDescEventsInRange(1).size());
    }

    public void testRemoveAllEvents() {
        RxEventStore eventStore = getEventStore();

        // Add 6 events
        eventStore.insertEvent(getEvent());
        eventStore.insertEvent(getEvent());
        eventStore.insertEvent(getEvent());
        eventStore.insertEvent(getEvent());
        eventStore.insertEvent(getEvent());
        eventStore.insertEvent(getEvent());

        assertEquals(6, eventStore.getSize());

        eventStore.removeAllEvents();

        assertEquals(0, eventStore.getSize());
    }

    public void testRemoveIndividualEvent() {
        RxEventStore eventStore = getEventStore();
        long id = eventStore.insertEvent(getEvent());
        boolean res = eventStore.removeEvent(id);

        assertEquals(0, eventStore.getSize());
        assertEquals(true, res);
    }

    public void testCloseDatabase() {
        RxEventStore eventStore = getEventStore();

        assertEquals(true, eventStore.isDatabaseOpen());

        eventStore.close();

        assertEquals(false, eventStore.isDatabaseOpen());
    }
}
