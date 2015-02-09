package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;
import android.util.Log;

import com.snowplowanalytics.snowplow.tracker.utils.payload.SchemaPayload;
import com.snowplowanalytics.snowplow.tracker.utils.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.storage.EventStore;
import com.snowplowanalytics.snowplow.tracker.storage.EventStoreHelper;

import java.util.List;
import java.util.Map;


public class EventStoreTest extends AndroidTestCase {

    public void testInsertPayload() throws Exception {
        TrackerPayload trackerPayload = new TrackerPayload();
        SchemaPayload schemaPayload = new SchemaPayload();
        trackerPayload.add("someKey", "someValue");
        trackerPayload.add("anotherKey", "anotherValue");
        schemaPayload.setSchema("iglu:com.snowplowanalytics.snowplow/example/jsonschema/1-0-0");
        schemaPayload.setData(trackerPayload);

        EventStore eventStore = new EventStore(getContext());
        long id = eventStore.insertPayload(schemaPayload);
        Map<String, Object> map = eventStore.getEvent(id);

        Log.d("EventStoreTest", map.get(EventStoreHelper.METADATA_EVENT_DATA).toString());
    }

    public void testGetPending() throws Exception {
        EventStore eventStore = new EventStore(getContext());
        List<Map<String, Object>> foo = eventStore.getAllNonPendingEvents();
        for (Map<String, Object> event : foo) {
            Log.d("EventStoreTest", event.toString());
        }
    }

    public void testRemoveAllEvents() throws Exception {
        EventStore eventStore = new EventStore(getContext());
        eventStore.removeAllEvents();
    }

    public void testSetPending() throws Exception {
        TrackerPayload trackerPayload = new TrackerPayload();
        SchemaPayload schemaPayload = new SchemaPayload();
        trackerPayload.add("someKey", "somethingElse");
        trackerPayload.add("anotherKey", "anotherSomethingElse");
        schemaPayload.setSchema("iglu:com.snowplowanalytics.snowplow/example/jsonschema/1-0-0");
        schemaPayload.setData(trackerPayload);

        EventStore eventStore = new EventStore(getContext());
        long id = eventStore.insertPayload(schemaPayload);
        eventStore.setPending(id);
    }
}
