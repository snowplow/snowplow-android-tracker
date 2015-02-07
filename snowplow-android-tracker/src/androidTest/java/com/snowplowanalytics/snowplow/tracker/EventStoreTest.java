package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

import java.util.Map;

import com.snowplowanalytics.snowplow.tracker.utils.payload.SchemaPayload;
import com.snowplowanalytics.snowplow.tracker.utils.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.storage.EventStore;

public class EventStoreTest extends AndroidTestCase {

    // Helper Methods

    private EventStore getEventStore() {
        EventStore eventStore = new EventStore(getContext());
        eventStore.removeAllEvents();
        return eventStore;
    }

    private SchemaPayload getEvent() {
        TrackerPayload trackerPayload = new TrackerPayload();
        SchemaPayload schemaPayload = new SchemaPayload();
        trackerPayload.add("someKey", "someValue");
        trackerPayload.add("anotherKey", "anotherValue");
        schemaPayload.setSchema("iglu:com.snowplowanalytics.snowplow/example/jsonschema/1-0-0");
        schemaPayload.setData(trackerPayload);

        return schemaPayload;
    }

    // Tests

    public void testInsertPayload() {
        EventStore eventStore = getEventStore();
        long id = eventStore.insertEvent(getEvent());
        long lastRowId = eventStore.getLastInsertedRowId();
        Map<String, Object> event = eventStore.getEvent(id);

        assertEquals(id, lastRowId);
        assertEquals(1, eventStore.getSize());
        assertNotNull(event);
    }

    public void testEventStoreQueries() {
        EventStore eventStore = getEventStore();
        eventStore.insertEvent(getEvent());

        assertEquals(1, eventStore.getAllEvents().size());
        assertEquals(1, eventStore.getDescEventsInRange(1).size());
    }

    public void testRemoveAllEvents() {
        EventStore eventStore = getEventStore();

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
        EventStore eventStore = getEventStore();
        long id = eventStore.insertEvent(getEvent());
        boolean res = eventStore.removeEvent(id);

        assertEquals(0, eventStore.getSize());
        assertEquals(true, res);
    }

    public void testCloseDatabase() {
        EventStore eventStore = getEventStore();

        assertEquals(true, eventStore.isDatabaseOpen());

        eventStore.close();

        assertEquals(false, eventStore.isDatabaseOpen());
    }

    // TODO: This test only fails in Travis - cannot replicate
//    public void testStressTestAddToDatabase() throws Exception{
//        EventStore eventStore = getEventStore();
//        for (int i = 0; i < 1000; i++)
//            eventStore.add(getEvent());
//
//        while (eventStore.getSize() != 1000) {
//            Thread.sleep(500);
//        }
//
//        assertEquals(1000, eventStore.getSize());
//    }
}
