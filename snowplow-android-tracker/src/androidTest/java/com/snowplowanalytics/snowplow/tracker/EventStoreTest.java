package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;
import android.util.Log;

import com.snowplowanalytics.snowplow.tracker.utils.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SchemaPayload;
import com.snowplowanalytics.snowplow.tracker.utils.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.storage.EventStore;
import com.snowplowanalytics.snowplow.tracker.storage.EventStoreHelper;

import java.util.List;
import java.util.Map;


public class EventStoreTest extends AndroidTestCase {

    private final BufferOption option = BufferOption.Instant;

    public void testInsertPayload() throws Exception {
        TrackerPayload trackerPayload = new TrackerPayload();
        SchemaPayload schemaPayload = new SchemaPayload();
        trackerPayload.add("someKey", "someValue");
        trackerPayload.add("anotherKey", "anotherValue");
        schemaPayload.setSchema("iglu:com.snowplowanalytics.snowplow/example/jsonschema/1-0-0");
        schemaPayload.setData(trackerPayload);

        EventStore eventStore = new EventStore(getContext(), option);
        long id = eventStore.insertPayload(schemaPayload);
        Map<String, Object> map = eventStore.getEvent(id);

        Log.d("EventStoreTest", map.get(EventStoreHelper.METADATA_EVENT_DATA).toString());
    }

    public void testRemoveAllEvents() throws Exception {
        EventStore eventStore = new EventStore(getContext(), option);
        eventStore.removeAllEvents();
    }
}
