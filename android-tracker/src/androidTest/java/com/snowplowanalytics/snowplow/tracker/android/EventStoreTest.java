package com.snowplowanalytics.snowplow.tracker.android;

import android.test.AndroidTestCase;
import android.util.Log;

import com.snowplowanalytics.snowplow.tracker.android.payload.SchemaPayload;
import com.snowplowanalytics.snowplow.tracker.android.payload.TrackerPayload;

import java.util.HashMap;


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
        HashMap<String, Object> map = (HashMap<String, Object>) eventStore.getEvent(id);

        eventStore.removeAllEvents();

        Log.d("EventStoreTest", map.toString());
    }
}