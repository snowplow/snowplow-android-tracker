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

public class EmitterTest extends SnowplowTestCase {

    private final static String TAG = Emitter.class.getSimpleName();

    
    public void setUp()  {
        
    }

    private Emitter getEmitter(HttpMethod method, BufferOption option, RequestSecurity security) {
        return new Emitter.EmitterBuilder(mockServerName(), getContext(), TestEmitter.class)
                .option(option)
                .method(method)
                .security(security)
                .callback(getCallback())
                .tick(5)
                .emptyLimit(1)
                .sendLimit(200)
                .byteLimitGet(20000)
                .byteLimitPost(25000)
                .build();
    }

    private RequestCallback getCallback() {
        return new RequestCallback() {
            @Override
            public void onSuccess(int successCount) {
                Logger.d(TAG, "Successful Sends: %s", successCount);
            }
            @Override
            public void onFailure(int successCount, int failureCount) {
                Logger.d(TAG, "Successful Sends: %s, Failed Sends: %s",
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
        Assert.assertEquals("http://" + mockServerName() + "/i", emitter.getEmitterUri());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTP);
        Assert.assertEquals("http://" + mockServerName() + "/com.snowplowanalytics.snowplow/tp2",
                emitter.getEmitterUri());

        emitter = getEmitter(HttpMethod.GET, BufferOption.DefaultGroup, RequestSecurity.HTTPS);
        Assert.assertEquals("https://" + mockServerName() + "/i", emitter.getEmitterUri());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTPS);
        Assert.assertEquals("https://" + mockServerName() + "/com.snowplowanalytics.snowplow/tp2",
                emitter.getEmitterUri());
    }

    public void testSecuritySet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals(RequestSecurity.HTTP, emitter.getRequestSecurity());

        emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTPS);
        Assert.assertEquals(RequestSecurity.HTTPS, emitter.getRequestSecurity());
    }

    public void testTickSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals(5, emitter.getEmitterTick());
    }

    public void testEmptyLimitSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals(1, emitter.getEmptyLimit());
    }

    public void testSendLimitSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals(200, emitter.getSendLimit());
    }

    public void testByteLimitGetSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals(20000, emitter.getByteLimitGet());
    }

    public void testByteLimitPostSet() {
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
        Assert.assertEquals(25000, emitter.getByteLimitPost());
    }
   
    public void testEmitSingleGetEvent() throws InterruptedException, IOException {
        setupMockServer();
        EmittableEvents emittableEvents = setupEmittableEvents(1);
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);
       
        LinkedList<RequestResult> result= emitter.performEmit(emittableEvents);

        RecordedRequest req = mockServer.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("GET", req.getMethod());
        assertEquals("/i?", req.getPath().substring(0, 3));
        
        assertEquals(1, result.size());
        assertEquals(0, result.getFirst().getEventIds().getFirst().intValue());
    }

    public void testEmitTwoGetEvents() throws InterruptedException, IOException {
        setupMockServer();
        EmittableEvents emittableEvents = setupEmittableEvents(2);
        Emitter emitter = getEmitter(HttpMethod.GET, BufferOption.Single, RequestSecurity.HTTP);

        LinkedList<RequestResult> result= emitter.performEmit(emittableEvents);

        RecordedRequest req = mockServer.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("GET", req.getMethod());
        assertEquals("/i?", req.getPath().substring(0, 3));

        req = mockServer.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("GET", req.getMethod());
        assertEquals("/i?", req.getPath().substring(0, 3));

        assertEquals(2, result.size());
        assertEquals(0, result.getFirst().getEventIds().getFirst().intValue());
        assertEquals(1, result.get(1).getEventIds().getFirst().intValue());
        
    }

    public void testEmitSinglePostEvent() throws InterruptedException, IOException, JSONException {
        setupMockServer();
        EmittableEvents emittableEvents = setupEmittableEvents(1);
        Emitter emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTP);

        LinkedList<RequestResult> result= emitter.performEmit(emittableEvents);

        RecordedRequest req = mockServer.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("POST", req.getMethod());
        assertEquals("/com.snowplowanalytics.snowplow/tp2", req.getPath());

        String body = req.getUtf8Body();
        JSONObject payload = new JSONObject(req.getUtf8Body());
        assertEquals(2, payload.length());
        assertEquals("iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-3",
                payload.getString("schema"));
        JSONArray payloadElements = payload.getJSONArray("data");
        assertEquals(1, payloadElements.length());
        JSONObject event1 = payloadElements.getJSONObject(0);
        assertEquals(2, event1.length());
        assertEquals(0, event1.getInt("a"));

        assertEquals(1, result.size());
        assertEquals(0, result.getFirst().getEventIds().getFirst().intValue());
    }

    public void testEmitTwoEventsPostAsGroup() throws InterruptedException, IOException, JSONException {
        setupMockServer();
        EmittableEvents emittableEvents = setupEmittableEvents(2);
        Emitter emitter = getEmitter(HttpMethod.POST, BufferOption.DefaultGroup, RequestSecurity.HTTP);

        LinkedList<RequestResult> result= emitter.performEmit(emittableEvents);

        RecordedRequest req = mockServer.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("POST", req.getMethod());
        assertEquals("/com.snowplowanalytics.snowplow/tp2", req.getPath());

        JSONObject payload = new JSONObject(req.getUtf8Body());
        assertEquals(2, payload.length());
        assertEquals("iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-3",
                payload.getString("schema"));
        JSONArray payloadElements = payload.getJSONArray("data");
        assertEquals(2, payloadElements.length());
        
        JSONObject event1 = payloadElements.getJSONObject(0);
        assertEquals(2, event1.length());
        assertEquals(0, event1.getInt("a"));

        JSONObject event2 = payloadElements.getJSONObject(1);
        assertEquals(2, event2.length());
        assertEquals(1, event2.getInt("a"));

        assertEquals(1, result.size());
        assertEquals(0, result.getFirst().getEventIds().getFirst().intValue());
        assertEquals(1, result.getFirst().getEventIds().get(1).intValue());
    }

    public void testEmitTwoEventsPostAsSingles() throws InterruptedException, IOException, JSONException {
        setupMockServer();
        EmittableEvents emittableEvents = setupEmittableEvents(2);
        Emitter emitter = getEmitter(HttpMethod.POST, BufferOption.Single, RequestSecurity.HTTP);

        LinkedList<RequestResult> result= emitter.performEmit(emittableEvents);

        RecordedRequest req = mockServer.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("POST", req.getMethod());
        assertEquals("/com.snowplowanalytics.snowplow/tp2", req.getPath());

        JSONObject payload = new JSONObject(req.getUtf8Body());
        assertEquals(2, payload.length());
        assertEquals("iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-3",
                payload.getString("schema"));
        JSONArray payloadElements = payload.getJSONArray("data");
        assertEquals(1, payloadElements.length());

        JSONObject event1 = payloadElements.getJSONObject(0);
        assertEquals(2, event1.length());
        assertEquals(0, event1.getInt("a"));

        // pull the second post off the queue

        RecordedRequest req2 = mockServer.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req2);
        assertEquals("POST", req2.getMethod());
        assertEquals("/com.snowplowanalytics.snowplow/tp2", req2.getPath());

        JSONObject payload2 = new JSONObject(req2.getUtf8Body());
        assertEquals(2, payload2.length());
        assertEquals("iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-3",
                payload2.getString("schema"));
        JSONArray payloadElements2 = payload2.getJSONArray("data");
        assertEquals(1, payloadElements2.length());
        
        JSONObject event2 = payloadElements2.getJSONObject(0);
        assertEquals(2, event2.length());
        assertEquals(1, event2.getInt("a"));

        assertEquals(2, result.size());
        assertEquals(0, result.getFirst().getEventIds().getFirst().intValue());
        assertEquals(1, result.get(1).getEventIds().getFirst().intValue());
    }

}
