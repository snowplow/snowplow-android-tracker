package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

import java.util.Map;

import com.snowplowanalytics.snowplow.tracker.utils.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.utils.payload.TrackerPayload;

import junit.framework.Assert;

public class EventStoreTest extends AndroidTestCase {

    // Helper Methods

    private EventStore getEventStore() {
        EventStore eventStore = new EventStore(getContext(), 250);
        eventStore.removeAllEvents();
        return eventStore;
    }

    private SelfDescribingJson getEvent() {
        TrackerPayload trackerPayload = new TrackerPayload();
        trackerPayload.add("someKey", "someValue");
        trackerPayload.add("anotherKey", "anotherValue");

        return new SelfDescribingJson(
                "iglu:com.snowplowanalytics.snowplow/example/jsonschema/1-0-0",
                trackerPayload);
    }

    // Tests

    public void testInsertPayload() {
        EventStore eventStore = getEventStore();
        long id = eventStore.insertEvent(getEvent());
        long lastRowId = eventStore.getLastInsertedRowId();
        Map<String, Object> event = eventStore.getEvent(id);

        assertEquals(id, lastRowId);
        Assert.assertEquals(1, eventStore.getSize());
        assertNotNull(event);
    }

    public void testEventStoreQueries() {
        EventStore eventStore = getEventStore();
        eventStore.insertEvent(getEvent());

        Assert.assertEquals(1, eventStore.getAllEvents().size());
        Assert.assertEquals(1, eventStore.getDescEventsInRange(1).size());
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

        Assert.assertEquals(6, eventStore.getSize());

        eventStore.removeAllEvents();

        Assert.assertEquals(0, eventStore.getSize());
    }

    public void testRemoveIndividualEvent() {
        EventStore eventStore = getEventStore();
        long id = eventStore.insertEvent(getEvent());
        boolean res = eventStore.removeEvent(id);

        Assert.assertEquals(0, eventStore.getSize());
        assertEquals(true, res);
    }

    public void testCloseDatabase() {
        EventStore eventStore = getEventStore();

        Assert.assertEquals(true, eventStore.isDatabaseOpen());

        eventStore.close();

        Assert.assertEquals(false, eventStore.isDatabaseOpen());
    }
}
