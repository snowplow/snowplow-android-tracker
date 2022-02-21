/*
 * Copyright (c) 2015-2021 Snowplow Analytics Ltd. All rights reserved.
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

import android.net.Uri;
import androidx.annotation.NonNull;
import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.internal.emitter.Emitter;
import com.snowplowanalytics.snowplow.network.NetworkConnection;
import com.snowplowanalytics.snowplow.emitter.BufferOption;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.Protocol;
import com.snowplowanalytics.snowplow.network.RequestCallback;
import com.snowplowanalytics.snowplow.internal.emitter.TLSVersion;
import com.snowplowanalytics.snowplow.network.Request;
import com.snowplowanalytics.snowplow.payload.Payload;
import com.snowplowanalytics.snowplow.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.network.RequestResult;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.snowplowanalytics.snowplow.emitter.BufferOption.DefaultGroup;
import static com.snowplowanalytics.snowplow.emitter.BufferOption.HeavyGroup;
import static com.snowplowanalytics.snowplow.emitter.BufferOption.Single;
import static com.snowplowanalytics.snowplow.network.HttpMethod.GET;
import static com.snowplowanalytics.snowplow.network.HttpMethod.POST;
import static com.snowplowanalytics.snowplow.network.Protocol.HTTP;
import static com.snowplowanalytics.snowplow.network.Protocol.HTTPS;

public class EmitterTest extends AndroidTestCase {

    // Builder Tests

    public void testHttpMethodSet() {
        Emitter emitter = new Emitter(getContext(), "com.acme", new Emitter.EmitterBuilder()
                .method(GET)
        );
        assertEquals(GET, emitter.getHttpMethod());

        emitter = new Emitter(getContext(), "com.acme", new Emitter.EmitterBuilder()
                .method(POST)
        );
        assertEquals(POST, emitter.getHttpMethod());
    }

    public void testBufferOptionSet() {
        Emitter emitter = new Emitter(getContext(), "com.acme", new Emitter.EmitterBuilder()
                .option(Single)
        );
        assertEquals(Single, emitter.getBufferOption());

        emitter = new Emitter(getContext(), "com.acme", new Emitter.EmitterBuilder()
                .option(DefaultGroup)
        );
        assertEquals(DefaultGroup, emitter.getBufferOption());

        emitter = new Emitter(getContext(), "com.acme", new Emitter.EmitterBuilder()
                .option(HeavyGroup)
        );
        assertEquals(HeavyGroup, emitter.getBufferOption());
    }

    public void testCallbackSet() {
        Emitter emitter = new Emitter(getContext(), "com.acme", new Emitter.EmitterBuilder()
                .callback(new RequestCallback() {
            @Override
            public void onSuccess(int successCount) {
            }

            @Override
            public void onFailure(int successCount, int failureCount) {
            }
        })
        );

        assertNotNull(emitter.getRequestCallback());
    }

    public void testUriSet() {
        String uri = "com.acme";

        Emitter emitter = new Emitter(getContext(), uri, new Emitter.EmitterBuilder()
                .method(GET)
                .security(HTTP)
                .option(Single)
        );
        assertEquals("http://" + uri + "/i", emitter.getEmitterUri());

        emitter = new Emitter(getContext(), uri, new Emitter.EmitterBuilder()
                .method(POST)
                .security(HTTP)
                .option(DefaultGroup)
        );
        assertEquals("http://" + uri + "/com.snowplowanalytics.snowplow/tp2",
                emitter.getEmitterUri());

        emitter = new Emitter(getContext(), uri, new Emitter.EmitterBuilder()
                .method(GET)
                .security(HTTPS)
                .option(DefaultGroup)
        );
        assertEquals("https://" + uri + "/i", emitter.getEmitterUri());

        emitter = new Emitter(getContext(), uri, new Emitter.EmitterBuilder()
                .method(POST)
                .security(HTTPS)
                .option(DefaultGroup)
        );
        assertEquals("https://" + uri + "/com.snowplowanalytics.snowplow/tp2", emitter.getEmitterUri());
    }

    public void testSecuritySet() {
        Emitter emitter = new Emitter(getContext(), "com.acme", new Emitter.EmitterBuilder()
                .security(HTTP)
        );
        assertEquals(HTTP, emitter.getRequestSecurity());

        emitter = new Emitter(getContext(), "com.acme", new Emitter.EmitterBuilder()
                .security(HTTPS)
        );
        assertEquals(Protocol.HTTPS, emitter.getRequestSecurity());
    }

    public void testTickSet() {
        Emitter emitter = new Emitter(getContext(), "com.acme", new Emitter.EmitterBuilder()
                .tick(0)
        );
        assertEquals(0, emitter.getEmitterTick());
    }

    public void testEmptyLimitSet() {
        Emitter emitter = new Emitter(getContext(), "com.acme", new Emitter.EmitterBuilder()
                .emptyLimit(0)
        );
        assertEquals(0, emitter.getEmptyLimit());
    }

    public void testSendLimitSet() {
        Emitter emitter = new Emitter(getContext(), "com.acme", new Emitter.EmitterBuilder()
                .sendLimit(200)
        );
        assertEquals(200, emitter.getSendLimit());
    }

    public void testByteLimitGetSet() {
        Emitter emitter = new Emitter(getContext(), "com.acme", new Emitter.EmitterBuilder()
                .byteLimitGet(20000)
        );
        assertEquals(20000, emitter.getByteLimitGet());
    }

    public void testByteLimitPostSet() {
        Emitter emitter = new Emitter(getContext(), "com.acme", new Emitter.EmitterBuilder()
                .byteLimitPost(25000)
        );
        assertEquals(25000, emitter.getByteLimitPost());
    }

    public void testUpdatingEmitterSettings() throws InterruptedException {
        String uri = "snowplowanalytics.com";
        Emitter emitter = new Emitter(getContext(), uri, new Emitter.EmitterBuilder()
                .option(Single)
                .method(POST)
                .security(HTTP)
                .tick(250)
                .emptyLimit(5)
                .sendLimit(200)
                .byteLimitGet(20000)
                .byteLimitPost(25000)
                .timeUnit(TimeUnit.MILLISECONDS)
                .eventStore(new MockEventStore())
        );

        assertFalse(emitter.getEmitterStatus());
        assertEquals(Single, emitter.getBufferOption());
        assertEquals("http://" + uri + "/com.snowplowanalytics.snowplow/tp2", emitter.getEmitterUri());
        emitter.setHttpMethod(GET);
        assertEquals("http://" + uri + "/i", emitter.getEmitterUri());
        emitter.setEmitterUri(uri);
        emitter.setRequestSecurity(Protocol.HTTPS);
        assertEquals("https://" + uri + "/i", emitter.getEmitterUri());
        emitter.setEmitterUri("com.acme");
        assertEquals("https://com.acme/i", emitter.getEmitterUri());
        emitter.setBufferOption(HeavyGroup);
        assertEquals(HeavyGroup, emitter.getBufferOption());

        emitter.flush();
        emitter.flush();
        Thread.sleep(500);

        assertTrue(emitter.getEmitterStatus());
        emitter.setHttpMethod(POST);
        assertEquals("https://com.acme/i", emitter.getEmitterUri());
        emitter.setEmitterUri("com/foo");
        assertEquals("https://com.acme/i", emitter.getEmitterUri());
        emitter.setBufferOption(DefaultGroup);
        assertEquals(HeavyGroup, emitter.getBufferOption());

        emitter.shutdown();

        Emitter customPathEmitter = new Emitter(getContext(), uri, new Emitter.EmitterBuilder()
                .option(Single)
                .method(POST)
                .security(HTTP)
                .tick(250)
                .emptyLimit(5)
                .sendLimit(200)
                .byteLimitGet(20000)
                .byteLimitPost(25000)
                .timeUnit(TimeUnit.MILLISECONDS)
                .customPostPath("com.acme.company/tpx")
                .eventStore(new MockEventStore())
        );
        assertEquals("com.acme.company/tpx", customPathEmitter.getCustomPostPath());
        assertEquals("http://" + uri + "/com.acme.company/tpx", customPathEmitter.getEmitterUri());

        customPathEmitter.shutdown();
    }

    // Emitting Tests

    public void testEmitEventWithBrokenNetworkConnectionDoesntFreezeEmitterStatus() throws InterruptedException {
        NetworkConnection networkConnection = new BrokenNetworkConnection();
        Emitter emitter = getEmitter(networkConnection, Single);

        emitter.add(generatePayloads(1).get(0));

        Thread.sleep(1000);

        assertFalse(emitter.getEmitterStatus());

        emitter.flush();
    }

    public void testEmitSingleGetEventWithSuccess() throws InterruptedException {
        MockNetworkConnection networkConnection = new MockNetworkConnection(GET,true);
        Emitter emitter = getEmitter(networkConnection, Single);

        emitter.add(generatePayloads(1).get(0));

        for (int i = 0; i < 10 && (networkConnection.sendingCount() < 1 || emitter.getEmitterStatus()); i++) {
            Thread.sleep(600);
        }

        assertEquals(1, networkConnection.previousResults.size());
        assertEquals(1, networkConnection.previousResults.get(0).size());
        assertTrue(networkConnection.previousResults.get(0).get(0).getSuccess());
        assertEquals(0, emitter.getEventStore().getSize());

        emitter.flush();
    }

    public void testEmitSingleGetEventWithNoSuccess() throws InterruptedException {
        MockNetworkConnection networkConnection = new MockNetworkConnection(GET,false);
        Emitter emitter = getEmitter(networkConnection, Single);

        emitter.add(generatePayloads(1).get(0));

        for (int i = 0; i < 10 && (networkConnection.sendingCount() < 1 || emitter.getEmitterStatus()); i++) {
            Thread.sleep(600);
        }

        assertEquals(1, networkConnection.previousResults.size());
        assertEquals(1, networkConnection.previousResults.get(0).size());
        assertFalse(networkConnection.previousResults.get(0).get(0).getSuccess());
        assertEquals(1, emitter.getEventStore().getSize());

        emitter.flush();
    }

    public void testEmitTwoGetEventsWithSuccess() throws InterruptedException {
        MockNetworkConnection networkConnection = new MockNetworkConnection(GET,true);
        Emitter emitter = getEmitter(networkConnection, Single);

        for (Payload payload : generatePayloads(2)) {
            emitter.add(payload);
        }

        for (int i = 0; i < 10 && (networkConnection.sendingCount() < 2 || emitter.getEmitterStatus()); i++) {
            Thread.sleep(600);
        }

        assertEquals(0, emitter.getEventStore().getSize());
        int totEvents = 0;
        for (List<RequestResult> results : networkConnection.previousResults) {
            for (RequestResult result : results) {
                assertTrue(result.getSuccess());
                totEvents += result.getEventIds().size();
            }
        }
        assertEquals(2, totEvents);

        emitter.flush();
    }

    public void testEmitTwoGetEventsWithNoSuccess() throws InterruptedException {
        MockNetworkConnection networkConnection = new MockNetworkConnection(GET,false);
        Emitter emitter = getEmitter(networkConnection, Single);

        for (Payload payload : generatePayloads(2)) {
            emitter.add(payload);
        }

        for (int i = 0; i < 10 && (networkConnection.sendingCount() < 2 || emitter.getEmitterStatus()); i++) {
            Thread.sleep(600);
        }

        assertEquals(2, emitter.getEventStore().getSize());
        for (List<RequestResult> results : networkConnection.previousResults) {
            for (RequestResult result : results) {
                assertFalse(result.getSuccess());
            }
        }
        // Can't check the total number of events sent as the Emitter stops to send if a request fails.

        emitter.flush();
    }

    public void testEmitSinglePostEventWithSuccess() throws InterruptedException {
        MockNetworkConnection networkConnection = new MockNetworkConnection(POST, true);
        Emitter emitter = getEmitter(networkConnection, Single);
        emitter.add(generatePayloads(1).get(0));

        for (int i = 0; i < 10 && networkConnection.sendingCount() < 1; i++) {
            Thread.sleep(600);
        }

        assertEquals(1, networkConnection.previousResults.size());
        assertEquals(1, networkConnection.previousResults.get(0).size());
        assertTrue(networkConnection.previousResults.get(0).get(0).getSuccess());
        assertEquals(0, emitter.getEventStore().getSize());

        emitter.flush();
    }

    public void testPauseAndResumeEmittingEvents() throws InterruptedException {
        MockNetworkConnection networkConnection = new MockNetworkConnection(POST, true);
        Emitter emitter = getEmitter(networkConnection, Single);

        emitter.pauseEmit();
        emitter.add(generatePayloads(1).get(0));

        Thread.sleep(1000);

        assertEquals(false, emitter.getEmitterStatus());
        assertEquals(0, networkConnection.sendingCount());
        assertEquals(0, networkConnection.previousResults.size());
        assertEquals(1, emitter.getEventStore().getSize());

        emitter.resumeEmit();

        for (int i = 0; i < 10 && networkConnection.sendingCount() < 1; i++) {
            Thread.sleep(600);
        }

        assertEquals(1, networkConnection.previousResults.size());
        assertEquals(1, networkConnection.previousResults.get(0).size());
        assertTrue(networkConnection.previousResults.get(0).get(0).getSuccess());
        assertEquals(0, emitter.getEventStore().getSize());

        emitter.flush();
    }

    // Emitter Builder

    public Emitter getEmitter(NetworkConnection networkConnection, BufferOption option) {
        return new Emitter(getContext(), "com.acme", new Emitter.EmitterBuilder()
                .networkConnection(networkConnection)
                .option(option)
                .tick(0)
                .emptyLimit(0)
                .sendLimit(200)
                .byteLimitGet(20000)
                .byteLimitPost(25000)
                .timeUnit(TimeUnit.SECONDS)
                .tls(EnumSet.of(TLSVersion.TLSv1_2))
                .eventStore(new MockEventStore())
        );
    }

    // Service methods

    public List<Payload> generatePayloads(int count) {
        ArrayList<Payload> payloads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TrackerPayload payload = new TrackerPayload();
            payload.add("a", String.valueOf(i));
            payloads.add(payload);
        }
        return payloads;
    }
}

// Mock classes

class BrokenNetworkConnection implements NetworkConnection {

    @NonNull
    @Override
    public List<RequestResult> sendRequests(@NonNull List<Request> requests) {
        throw new UnsupportedOperationException("Broken NetworkConnection");
    }

    @NonNull
    @Override
    public HttpMethod getHttpMethod() {
        throw new UnsupportedOperationException("Broken NetworkConnection");
    }

    @NonNull
    @Override
    public Uri getUri() {
        throw new UnsupportedOperationException("Broken NetworkConnection");
    }
}

class MockNetworkConnection implements NetworkConnection {
    public boolean successfulConnection;
    public HttpMethod httpMethod;

    public final List<List<RequestResult>> previousResults = new ArrayList<>();

    public MockNetworkConnection(HttpMethod httpMethod, boolean successfulConnection) {
        this.httpMethod = httpMethod;
        this.successfulConnection = successfulConnection;
    }

    public int sendingCount() {
        return previousResults.size();
    }

    @NonNull
    @Override
    public List<RequestResult> sendRequests(@NonNull List<Request> requests) {
        List<RequestResult> requestResults = new ArrayList<>(requests.size());
        for (Request request : requests) {
            boolean isSuccessful = request.oversize || successfulConnection;
            RequestResult result = new RequestResult(isSuccessful, request.emitterEventIds);
            Logger.v("MockNetworkConnection", "Sent: %s with success: %s", request.emitterEventIds, Boolean.valueOf(isSuccessful).toString());
            requestResults.add(result);
        }
        previousResults.add(requestResults);
        return requestResults;
    }

    @NonNull
    @Override
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    @NonNull
    @Override
    public Uri getUri() {
        return Uri.parse("http://fake-url.com");
    }
}

