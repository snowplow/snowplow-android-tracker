/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package com.snowplowanalytics.snowplow.tracker;

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.emitter.ReadyRequest;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestCallback;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestSecurity;
import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.storage.EventStore;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.emitter.EmittableEvents;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class EmitterTest extends AndroidTestCase {

    // Builder Tests

    public void testHttpMethodSet() {
        Emitter emitter = getEmitter("com.acme", HttpMethod.GET, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        Assert.assertEquals(HttpMethod.GET, emitter.getHttpMethod());

        emitter = getEmitter("com.acme", HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        Assert.assertEquals(HttpMethod.POST, emitter.getHttpMethod());
    }

    public void testBufferOptionSet() {
        Emitter emitter = getEmitter("com.acme", HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals(BufferOption.Single, emitter.getBufferOption());

        emitter = getEmitter("com.acme", HttpMethod.GET, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        Assert.assertEquals(BufferOption.DefaultGroup, emitter.getBufferOption());

        emitter.setBufferOption(BufferOption.HeavyGroup);
        Assert.assertEquals(BufferOption.HeavyGroup, emitter.getBufferOption());
    }

    public void testCallbackSet() {
        Emitter emitter = new Emitter.EmitterBuilder("com.acme", getContext()).callback(new RequestCallback() {
            @Override
            public void onSuccess(int successCount) {}
            @Override
            public void onFailure(int successCount, int failureCount) {}
        }).build();

        assertNotNull(emitter.getRequestCallback());
    }

    public void testUriSet() throws IOException {
        MockWebServer mockServer = getMockServer();
        String uri = getMockServerURI(mockServer);

        Emitter emitter = getEmitter(uri, HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals("http://" + getMockServerURI(mockServer) + "/i", emitter.getEmitterUri());

        emitter = getEmitter(uri, HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        Assert.assertEquals("http://" + getMockServerURI(mockServer) + "/com.snowplowanalytics.snowplow/tp2",
                emitter.getEmitterUri());

        emitter = getEmitter(uri, HttpMethod.GET, BufferOption.DefaultGroup, RequestSecurity.HTTPS);
        Assert.assertEquals("https://" + getMockServerURI(mockServer) + "/i", emitter.getEmitterUri());

        emitter = getEmitter(uri, HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTPS);
        Assert.assertEquals("https://" + getMockServerURI(mockServer) + "/com.snowplowanalytics.snowplow/tp2",
                emitter.getEmitterUri());

        mockServer.shutdown();
    }

    public void testSecuritySet() {
        Emitter emitter = getEmitter("com.acme", HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals(RequestSecurity.HTTP, emitter.getRequestSecurity());

        emitter = getEmitter("com.acme", HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTPS);
        Assert.assertEquals(RequestSecurity.HTTPS, emitter.getRequestSecurity());
    }

    public void testTickSet() {
        Emitter emitter = getEmitter("com.acme", HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals(0, emitter.getEmitterTick());
    }

    public void testEmptyLimitSet() {
        Emitter emitter = getEmitter("com.acme", HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals(0, emitter.getEmptyLimit());
    }

    public void testSendLimitSet() {
        Emitter emitter = getEmitter("com.acme", HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals(200, emitter.getSendLimit());
    }

    public void testByteLimitGetSet() {
        Emitter emitter = getEmitter("com.acme", HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals(20000, emitter.getByteLimitGet());
    }

    public void testByteLimitPostSet() {
        Emitter emitter = getEmitter("com.acme", HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals(25000, emitter.getByteLimitPost());
    }

    public void testUpdatingEmitterSettings() throws InterruptedException, IOException, JSONException {
        MockWebServer mockServer = getMockServer();
        Emitter emitter = new Emitter.EmitterBuilder(getMockServerURI(mockServer), getContext())
                .option(BufferOption.Single)
                .method(HttpMethod.POST)
                .security(RequestSecurity.HTTP)
                .tick(250)
                .emptyLimit(5)
                .sendLimit(200)
                .byteLimitGet(20000)
                .byteLimitPost(25000)
                .timeUnit(TimeUnit.MILLISECONDS)
                .build();

        assertFalse(emitter.getEmitterStatus());
        assertEquals(BufferOption.Single, emitter.getBufferOption());
        assertEquals("http://" + getMockServerURI(mockServer) + "/com.snowplowanalytics.snowplow/tp2", emitter.getEmitterUri());
        emitter.setHttpMethod(HttpMethod.GET);
        assertEquals("http://" + getMockServerURI(mockServer) + "/i", emitter.getEmitterUri());
        emitter.setRequestSecurity(RequestSecurity.HTTPS);
        assertEquals("https://" + getMockServerURI(mockServer) + "/i", emitter.getEmitterUri());
        emitter.setEmitterUri("com.acme");
        assertEquals("https://com.acme/i", emitter.getEmitterUri());
        emitter.setBufferOption(BufferOption.HeavyGroup);
        assertEquals(BufferOption.HeavyGroup, emitter.getBufferOption());

        emitter.flush();
        emitter.flush();
        Thread.sleep(500);

        assertTrue(emitter.getEmitterStatus());
        emitter.setHttpMethod(HttpMethod.POST);
        assertEquals("https://com.acme/i", emitter.getEmitterUri());
        emitter.setRequestSecurity(RequestSecurity.HTTP);
        assertEquals("https://com.acme/i", emitter.getEmitterUri());
        emitter.setEmitterUri("com/foo");
        assertEquals("https://com.acme/i", emitter.getEmitterUri());
        emitter.setBufferOption(BufferOption.DefaultGroup);
        assertEquals(BufferOption.HeavyGroup, emitter.getBufferOption());

        emitter.shutdown();

        mockServer.shutdown();
    }

    // Emitting Tests

    private void assertGETRequest(RecordedRequest req) {
        assertNotNull(req);
        assertEquals("GET", req.getMethod());
        assertEquals("Keep-Alive", req.getHeader("Connection"));
        assertEquals("/i?", req.getPath().substring(0, 3));
    }

    private JSONObject assertPOSTRequest(RecordedRequest req) throws JSONException {
        assertNotNull(req);
        assertEquals("POST", req.getMethod());
        assertEquals("application/json; charset=utf-8", req.getHeader("Content-Type"));
        assertEquals("Keep-Alive", req.getHeader("Connection"));
        assertEquals("/com.snowplowanalytics.snowplow/tp2", req.getPath());

        JSONObject payload = new JSONObject(req.getBody().readUtf8());
        assertEquals(2, payload.length());
        assertEquals(
                "iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-3",
                payload.getString("schema")
        );

        return payload;
    }
   
    public void testEmitSingleGetEvent() throws InterruptedException, IOException {
        MockWebServer mockServer = getMockServer();
        EmittableEvents emittableEvents = getEmittableEvents(mockServer, 1);
        Emitter emitter = getEmitter(getMockServerURI(mockServer), HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
       
        LinkedList<RequestResult> result = emitter.performAsyncEmit(emitter.buildRequests(emittableEvents));
        assertEquals(1, result.size());
        assertEquals(0, result.getFirst().getEventIds().getFirst().intValue());

        RecordedRequest req = mockServer.takeRequest(2, TimeUnit.SECONDS);
        assertGETRequest(req);

        mockServer.shutdown();
    }

    public void testEmitTwoGetEvents() throws InterruptedException, IOException {
        MockWebServer mockServer = getMockServer();
        EmittableEvents emittableEvents = getEmittableEvents(mockServer, 2);
        Emitter emitter = getEmitter(getMockServerURI(mockServer), HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);

        LinkedList<RequestResult> result = emitter.performAsyncEmit(emitter.buildRequests(emittableEvents));
        assertEquals(2, result.size());
        assertEquals(0, result.getFirst().getEventIds().getFirst().intValue());
        assertEquals(1, result.get(1).getEventIds().getFirst().intValue());

        RecordedRequest req = mockServer.takeRequest(2, TimeUnit.SECONDS);
        assertGETRequest(req);

        req = mockServer.takeRequest(2, TimeUnit.SECONDS);
        assertGETRequest(req);

        mockServer.shutdown();
    }

    public void testEmitSinglePostEvent() throws InterruptedException, IOException, JSONException {
        MockWebServer mockServer = getMockServer();
        EmittableEvents emittableEvents = getEmittableEvents(mockServer, 1);
        Emitter emitter = getEmitter(getMockServerURI(mockServer), HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTP);

        LinkedList<RequestResult> result = emitter.performAsyncEmit(emitter.buildRequests(emittableEvents));
        assertEquals(1, result.size());
        assertEquals(0, result.getFirst().getEventIds().getFirst().intValue());

        RecordedRequest req = mockServer.takeRequest(2, TimeUnit.SECONDS);
        JSONObject payload = assertPOSTRequest(req);

        JSONArray data = payload.getJSONArray("data");
        assertEquals(1, data.length());
        JSONObject event = data.getJSONObject(0);
        assertEquals(2, event.length());
        assertEquals(0, event.getInt("a"));

        mockServer.shutdown();
    }

    public void testEmitTwoEventsPostAsGroup() throws InterruptedException, IOException, JSONException {
        MockWebServer mockServer = getMockServer();
        EmittableEvents emittableEvents = getEmittableEvents(mockServer, 2);
        Emitter emitter = getEmitter(getMockServerURI(mockServer), HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTP);

        LinkedList<RequestResult> result = emitter.performAsyncEmit(emitter.buildRequests(emittableEvents));
        assertEquals(1, result.size());
        assertEquals(0, result.getFirst().getEventIds().getFirst().intValue());
        assertEquals(1, result.getFirst().getEventIds().get(1).intValue());

        RecordedRequest req = mockServer.takeRequest(2, TimeUnit.SECONDS);
        JSONObject payload = assertPOSTRequest(req);

        JSONArray data = payload.getJSONArray("data");
        assertEquals(2, data.length());
        JSONObject event1 = data.getJSONObject(0);
        assertEquals(2, event1.length());
        assertEquals(0, event1.getInt("a"));
        JSONObject event2 = data.getJSONObject(1);
        assertEquals(2, event2.length());
        assertEquals(1, event2.getInt("a"));

        mockServer.shutdown();
    }

    public void testEmitTwoEventsPostAsSingles() throws InterruptedException, IOException, JSONException {
        MockWebServer mockServer = getMockServer();
        EmittableEvents emittableEvents = getEmittableEvents(mockServer, 2);
        Emitter emitter = getEmitter(getMockServerURI(mockServer), HttpMethod.POST, BufferOption.Single, RequestSecurity.HTTP);

        LinkedList<RequestResult> result = emitter.performAsyncEmit(emitter.buildRequests(emittableEvents));
        assertEquals(2, result.size());
        assertEquals(0, result.getFirst().getEventIds().getFirst().intValue());
        assertEquals(1, result.get(1).getEventIds().getFirst().intValue());

        // Process first request
        RecordedRequest req = mockServer.takeRequest(2, TimeUnit.SECONDS);
        JSONObject payload = assertPOSTRequest(req);

        JSONArray data1 = payload.getJSONArray("data");
        assertEquals(1, data1.length());
        JSONObject event1 = data1.getJSONObject(0);
        assertEquals(2, event1.length());

        // Process second request
        RecordedRequest req2 = mockServer.takeRequest(2, TimeUnit.SECONDS);
        JSONObject payload2 = assertPOSTRequest(req2);

        JSONArray data2 = payload2.getJSONArray("data");
        assertEquals(1, data2.length());
        JSONObject event2 = data2.getJSONObject(0);
        assertEquals(2, event2.length());

        mockServer.shutdown();
    }

    public void testEmitOversizeEvent() throws InterruptedException, IOException, JSONException {
        Executor.setThreadCount(10);
        Executor.shutdown();

        MockWebServer mockServer = getMockServer();
        EmittableEvents emittableEvents = getBadEmittableEvents(mockServer, 10);

        Emitter emitter = new Emitter.EmitterBuilder(getMockServerURI(mockServer), getContext())
                .option(BufferOption.Single)
                .method(HttpMethod.GET)
                .tick(0)
                .callback(getCallback())
                .emptyLimit(0)
                .sendLimit(10)
                .byteLimitGet(5)
                .build();

        LinkedList<RequestResult> results = emitter.performAsyncEmit(emitter.buildRequests(emittableEvents));
        for (RequestResult res : results) {
            assertTrue(res.getSuccess());
        }

        mockServer.shutdown();
    }

    public void testEmitOnlyFailingEvents() throws InterruptedException, IOException, JSONException {
        Executor.setThreadCount(10);
        Executor.shutdown();

        MockWebServer mockServer = getMockServer();
        getBadEmittableEvents(mockServer, 10);

        new EventStore(getContext(), 5).removeAllEvents();
        Emitter emitter = new Emitter.EmitterBuilder(getMockServerURI(mockServer), getContext())
                .option(BufferOption.Single)
                .method(HttpMethod.GET)
                .tick(1)
                .callback(getCallback())
                .emptyLimit(5)
                .sendLimit(10)
                .build();

        TrackerPayload payload = new TrackerPayload();
        payload.add("key", "value");
        payload.add("e", "pv");

        for (int i = 0; i < 10; i++) {
            emitter.add(payload);
        }
        Thread.sleep(1000);

        for (int i = 0; i < 10; i++) {
            mockServer.takeRequest(20, TimeUnit.SECONDS);
        }

        emitter.getEventStore().removeAllEvents();
        emitter.shutdown();

        mockServer.shutdown();
    }

    public void testEmitOnlySuccessEvents() throws InterruptedException, IOException, JSONException {
        Executor.setThreadCount(10);
        Executor.shutdown();

        MockWebServer mockServer = getMockServer();
        getEmittableEvents(mockServer, 10);

        new EventStore(getContext(), 5).removeAllEvents();
        Emitter emitter = new Emitter.EmitterBuilder(getMockServerURI(mockServer), getContext())
                .option(BufferOption.Single)
                .method(HttpMethod.GET)
                .tick(1)
                .callback(getCallback())
                .emptyLimit(5)
                .sendLimit(10)
                .build();

        TrackerPayload payload = new TrackerPayload();
        payload.add("key", "value");
        payload.add("e", "pv");

        for (int i = 0; i < 10; i++) {
            emitter.add(payload);
        }
        Thread.sleep(1000);

        for (int i = 0; i < 10; i++) {
            mockServer.takeRequest(20, TimeUnit.SECONDS);
        }

        emitter.getEventStore().removeAllEvents();
        emitter.shutdown();

        mockServer.shutdown();
    }

    public void testEmitMixedEvents() throws InterruptedException, IOException, JSONException {
        Executor.setThreadCount(10);
        Executor.shutdown();

        MockWebServer mockServer = getMockServer();
        getEmittableEvents(mockServer, 5);
        getBadEmittableEvents(mockServer, 5);

        new EventStore(getContext(), 5).removeAllEvents();
        Emitter emitter = new Emitter.EmitterBuilder(getMockServerURI(mockServer), getContext())
                .option(BufferOption.Single)
                .method(HttpMethod.GET)
                .tick(1)
                .callback(getCallback())
                .emptyLimit(5)
                .sendLimit(10)
                .build();

        TrackerPayload payload = new TrackerPayload();
        payload.add("key", "value");
        payload.add("e", "pv");

        for (int i = 0; i < 10; i++) {
            emitter.add(payload);
        }
        Thread.sleep(1000);

        for (int i = 0; i < 10; i++) {
            mockServer.takeRequest(20, TimeUnit.SECONDS);
        }

        emitter.getEventStore().removeAllEvents();
        emitter.shutdown();

        mockServer.shutdown();
    }

    public void testBuildingOversizeEvents() throws InterruptedException, IOException, JSONException {
        Executor.setThreadCount(10);
        Executor.shutdown();

        MockWebServer mockServer = getMockServer();
        EmittableEvents emittableEvents = getEmittableEvents(mockServer, 10);
        Emitter emitter = new Emitter.EmitterBuilder(getMockServerURI(mockServer), getContext())
                .option(BufferOption.Single)
                .method(HttpMethod.GET)
                .tick(0)
                .callback(getCallback())
                .emptyLimit(0)
                .sendLimit(10)
                .byteLimitGet(5)
                .byteLimitPost(5)
                .build();

        LinkedList<ReadyRequest> requests = emitter.buildRequests(emittableEvents);
        assertEquals(10, requests.size());

        emitter.setHttpMethod(HttpMethod.POST);
        requests = emitter.buildRequests(emittableEvents);
        assertEquals(10, requests.size());

        emitter = new Emitter.EmitterBuilder(getMockServerURI(mockServer), getContext())
                .option(BufferOption.HeavyGroup)
                .method(HttpMethod.POST)
                .tick(0)
                .emptyLimit(0)
                .sendLimit(10)
                .byteLimitGet(5)
                .byteLimitPost(500)
                .build();

        requests = emitter.buildRequests(emittableEvents);
        assertEquals(2, requests.size());

        mockServer.shutdown();
    }

    // Emitter Builder

    public Emitter getEmitter(String uri, HttpMethod method, BufferOption option, RequestSecurity security) {
        return new Emitter.EmitterBuilder(uri, getContext())
                .option(option)
                .method(method)
                .security(security)
                .tick(0)
                .emptyLimit(0)
                .sendLimit(200)
                .byteLimitGet(20000)
                .byteLimitPost(25000)
                .timeUnit(TimeUnit.SECONDS)
                .build();
    }

    public RequestCallback getCallback() {
        return new RequestCallback() {
            @Override
            public void onSuccess(int successCount) {}
            @Override
            public void onFailure(int successCount, int failureCount) {}
        };
    }

    // Mock Server

    public MockWebServer getMockServer() throws IOException {
        MockWebServer mockServer = new MockWebServer();
        mockServer.start();
        return mockServer;
    }

    @SuppressLint("DefaultLocale")
    public String getMockServerURI(MockWebServer mockServer) {
        if (mockServer != null) {
            return String.format("%s:%d", mockServer.getHostName(), mockServer.getPort());
        }
        return null;
    }

    public EmittableEvents getEmittableEvents(MockWebServer mockServer, int count) {
        ArrayList<Payload> events = new ArrayList<>();
        LinkedList<Long> eventIds = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            TrackerPayload payload = new TrackerPayload();
            payload.add("a", String.valueOf(i));

            events.add(payload);
            eventIds.add((long) i);

            mockServer.enqueue(new MockResponse());
        }
        return new EmittableEvents(events, eventIds);
    }

    public EmittableEvents getBadEmittableEvents(MockWebServer mockServer, int count) {
        ArrayList<Payload> events = new ArrayList<>();
        LinkedList<Long> eventIds = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            TrackerPayload payload = new TrackerPayload();
            payload.add("a", String.valueOf(i));

            events.add(payload);
            eventIds.add((long) i);

            mockServer.enqueue(new MockResponse().setResponseCode(400));
        }
        return new EmittableEvents(events, eventIds);
    }
}
