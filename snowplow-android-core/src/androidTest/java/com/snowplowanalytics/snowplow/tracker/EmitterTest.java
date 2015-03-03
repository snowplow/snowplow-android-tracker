package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.utils.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.utils.storage.EmittableEvents;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class EmitterTest extends AndroidTestCase {

    private final static String TAG = Emitter.class.getSimpleName();

    private MockWebServer server;
    private String testHost = "nothing";
    private Integer testPort = 8888;
    private String testServer = "nothing:8888";
    
    public void setUp()  {
        
    }
    
    public void startMockServer() throws IOException {
        server = new MockWebServer();
        server.play();
        testHost = server.getHostName();
        testPort = server.getPort();
        testServer = String.format("%s:%d", testHost, testPort);
    }

    public EmittableEvents setupEmittableEvents(int count) {
        ArrayList<Payload> events = new ArrayList<Payload>();
        LinkedList<Long> eventIds = new LinkedList<Long>();
        for (int i = 0; i < count; i++) {
            TrackerPayload payload = new TrackerPayload();
            payload.add("a", String.valueOf(i));
            events.add(payload);
            eventIds.add(new Long(i));
            server.enqueue(new MockResponse());
        }
        return new EmittableEvents(events, eventIds);
    }


    public void tearDown() throws IOException {
        if (server != null) {
            server.shutdown(); 
            server = null;
        }
    }

    private Emitter getEmitter(HttpMethod method, BufferOption option, RequestSecurity security) {
        return new com.snowplowanalytics.snowplow.tracker.Emitter.EmitterBuilder(testServer, getContext(), TestEmitter.class)
                .option(option)
                .method(method)
                .security(security)
                .callback(getCallback())
                .build();
    }
    
    private Emitter getMethodEmitter() {
        return getEmitter(HttpMethod.GET, BufferOption.DefaultGroup, RequestSecurity.HTTP);
    }
    
    private Emitter postMethodEmitter() {
        return getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTP);
    }
    
    private HashMap<String,Object> abMap() {
        HashMap<String,Object> map = new HashMap<>();
        map.put("a", "b");
        return map;
    }

    private HashMap<String,Object> nestedAbMap() {
        HashMap<String,Object> map = new HashMap<>();
        map.put("a", abMap());
        return map;
    }

    private RequestCallback getCallback() {
        return new RequestCallback() {
            @Override
            public void onSuccess(int successCount) {
                Logger.ifDebug(TAG, "Successful Sends: %s", successCount);
            }
            @Override
            public void onFailure(int successCount, int failureCount) {
                Logger.ifDebug(TAG,
                        "Successful Sends: %s, Failed Sends: %s",
                        successCount,
                        failureCount);
            }
        };
    }

    // Tests

    public void testHttpMethodSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        Assert.assertEquals(HttpMethod.GET, emitter.getHttpMethod());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        Assert.assertEquals(HttpMethod.POST, emitter.getHttpMethod());
    }

    public void testBufferOptionSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals(BufferOption.Single, emitter.getBufferOption());

        emitter = getEmitter(HttpMethod.GET, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        Assert.assertEquals(BufferOption.DefaultGroup, emitter.getBufferOption());

        emitter.setBufferOption(BufferOption.HeavyGroup);
        Assert.assertEquals(BufferOption.HeavyGroup, emitter.getBufferOption());
    }

    public void testCallbackSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        assertNotNull(emitter.getRequestCallback());
    }

    public void testUriSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals("http://" + testServer + "/i", emitter.getEmitterUri());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        Assert.assertEquals("http://" + testServer + "/com.snowplowanalytics.snowplow/tp2",
                emitter.getEmitterUri());

        emitter = getEmitter(HttpMethod.GET, BufferOption.DefaultGroup, RequestSecurity.HTTPS);
        Assert.assertEquals("https://" + testServer + "/i", emitter.getEmitterUri());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTPS);
        Assert.assertEquals("https://" + testServer + "/com.snowplowanalytics.snowplow/tp2",
                emitter.getEmitterUri());
    }

    public void testSecuritySet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals(RequestSecurity.HTTP, emitter.getRequestSecurity());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTPS);
        Assert.assertEquals(RequestSecurity.HTTPS, emitter.getRequestSecurity());
    }
   
    public void testEmitSingleGetEvent() throws InterruptedException, IOException {
        startMockServer();
        EmittableEvents emittableEvents = setupEmittableEvents(1);
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
       
        LinkedList<RequestResult> result= emitter.performEmit(emittableEvents);

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("GET", req.getMethod());
        assertEquals("/i?a=0", req.getPath());
        
        assertEquals(1, result.size());
        assertEquals(0, result.getFirst().getEventIds().getFirst().intValue());
    }

    public void testEmitTwoGetEvents() throws InterruptedException, IOException {
        startMockServer();
        EmittableEvents emittableEvents = setupEmittableEvents(2);
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);

        LinkedList<RequestResult> result= emitter.performEmit(emittableEvents);

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("GET", req.getMethod());
        assertEquals("/i?a=0", req.getPath());

        req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("GET", req.getMethod());
        assertEquals("/i?a=1", req.getPath());

        assertEquals(2, result.size());
        assertEquals(0, result.getFirst().getEventIds().getFirst().intValue());
        assertEquals(1, result.get(1).getEventIds().getFirst().intValue());
        
    }

    public void testEmitSinglePostEvent() throws InterruptedException, IOException, JSONException {
        startMockServer();
        EmittableEvents emittableEvents = setupEmittableEvents(1);
        Emitter emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTP);

        LinkedList<RequestResult> result= emitter.performEmit(emittableEvents);

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("POST", req.getMethod());
        assertEquals("/com.snowplowanalytics.snowplow/tp2", req.getPath());

        String body = req.getUtf8Body();
        JSONObject payload = new JSONObject(req.getUtf8Body());
        assertEquals(2, payload.length());
        assertEquals("iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-2", payload.getString("schema"));
        JSONArray payloadElements = payload.getJSONArray("data");
        assertEquals(1, payloadElements.length());
        JSONObject event1 = payloadElements.getJSONObject(0);
        assertEquals(1, event1.length());
        assertEquals(0, event1.getInt("a"));

        assertEquals(1, result.size());
        assertEquals(0, result.getFirst().getEventIds().getFirst().intValue());
    }

    public void testEmitTwoEventsPostAsGroup() throws InterruptedException, IOException, JSONException {
        startMockServer();
        EmittableEvents emittableEvents = setupEmittableEvents(2);
        Emitter emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTP);

        LinkedList<RequestResult> result= emitter.performEmit(emittableEvents);

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("POST", req.getMethod());
        assertEquals("/com.snowplowanalytics.snowplow/tp2", req.getPath());

        String body = req.getUtf8Body();
        JSONObject payload = new JSONObject(req.getUtf8Body());
        assertEquals(2, payload.length());
        assertEquals("iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-2", payload.getString("schema"));
        JSONArray payloadElements = payload.getJSONArray("data");
        assertEquals(2, payloadElements.length());
        
        JSONObject event1 = payloadElements.getJSONObject(0);
        assertEquals(1, event1.length());
        assertEquals(0, event1.getInt("a"));

        JSONObject event2 = payloadElements.getJSONObject(1);
        assertEquals(1, event2.length());
        assertEquals(1, event2.getInt("a"));

        assertEquals(1, result.size());
        assertEquals(0, result.getFirst().getEventIds().getFirst().intValue());
        assertEquals(1, result.getFirst().getEventIds().get(1).intValue());
    }

    public void testEmitTwoEventsPostAsSingles() throws InterruptedException, IOException, JSONException {
        startMockServer();
        EmittableEvents emittableEvents = setupEmittableEvents(2);
        Emitter emitter = getEmitter(HttpMethod.POST, BufferOption.Single, RequestSecurity.HTTP);

        LinkedList<RequestResult> result= emitter.performEmit(emittableEvents);

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("POST", req.getMethod());
        assertEquals("/com.snowplowanalytics.snowplow/tp2", req.getPath());

        String body = req.getUtf8Body();
        JSONObject payload = new JSONObject(req.getUtf8Body());
        assertEquals(2, payload.length());
        assertEquals("iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-2", payload.getString("schema"));
        JSONArray payloadElements = payload.getJSONArray("data");
        assertEquals(1, payloadElements.length());

        JSONObject event1 = payloadElements.getJSONObject(0);
        assertEquals(1, event1.length());
        assertEquals(0, event1.getInt("a"));

        // pull the second post off the queue

        RecordedRequest req2 = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req2);
        assertEquals("POST", req2.getMethod());
        assertEquals("/com.snowplowanalytics.snowplow/tp2", req2.getPath());

        JSONObject payload2 = new JSONObject(req2.getUtf8Body());
        assertEquals(2, payload2.length());
        assertEquals("iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-2", payload2.getString("schema"));
        JSONArray payloadElements2 = payload2.getJSONArray("data");
        assertEquals(1, payloadElements2.length());
        
        JSONObject event2 = payloadElements2.getJSONObject(0);
        assertEquals(1, event2.length());
        assertEquals(1, event2.getInt("a"));

        assertEquals(2, result.size());
        assertEquals(0, result.getFirst().getEventIds().getFirst().intValue());
        assertEquals(1, result.get(1).getEventIds().getFirst().intValue());
    }

}
