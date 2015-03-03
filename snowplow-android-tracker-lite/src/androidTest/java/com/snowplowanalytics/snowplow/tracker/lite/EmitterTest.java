package com.snowplowanalytics.snowplow.tracker.lite;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.*;
import com.snowplowanalytics.snowplow.tracker.Emitter;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.utils.storage.EmittableEvents;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class EmitterTest extends SnowplowLiteTestCase {
    
    private final static String TAG = Emitter.class.getSimpleName();
    
    public void testDefaultEmitterShouldBeLiteEmitter() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        assertEquals(com.snowplowanalytics.snowplow.tracker.lite.Emitter.class, emitter.getClass());
    }

    public void testEmitSinglePostEvent() throws InterruptedException, IOException, JSONException {
        setupMockServer();
        mockServer.enqueue(new MockResponse());     

        Emitter emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        

        HashMap<String,String>map = new HashMap<>();
        map.put("a", "b");
        
        emitter.add(new SelfDescribingJson("schema", map));

        RecordedRequest req = mockServer.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("POST", req.getMethod());
        assertEquals("/com.snowplowanalytics.snowplow/tp2", req.getPath());

        //String body = req.getUtf8Body();
        JSONObject payload = new JSONObject(req.getUtf8Body());
        assertEquals(2, payload.length());
        assertEquals("iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-2", payload.getString("schema"));
        JSONArray payloadElements = payload.getJSONArray("data");
        assertEquals(1, payloadElements.length());
        JSONObject event1 = payloadElements.getJSONObject(0);
        assertEquals(2, event1.length());
        assertEquals(0, event1.getInt("a"));

        //assertEquals(1, result.size());
        //assertEquals(0, result.getFirst().getEventIds().getFirst().intValue());
    }
//
//    public void testOnePingOnly() throws InterruptedException, JSONException {
//        emitter.add(payload);
//        RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
//        assertNotNull(req);
//        assertEquals("POST", req.getMethod());
//        JSONObject json = new JSONObject(req.getUtf8Body());
//        assertEquals("iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-2", json.getString("schema"));
//        // check more...
//    }


}
