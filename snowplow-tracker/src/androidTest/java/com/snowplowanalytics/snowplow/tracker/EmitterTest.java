/*
 * Copyright (c) 2015-2020 Snowplow Analytics Ltd. All rights reserved.
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

import com.snowplowanalytics.snowplow.tracker.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.emitter.EmitterEvent;
import com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestCallback;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestSecurity;
import com.snowplowanalytics.snowplow.tracker.emitter.TLSVersion;
import com.snowplowanalytics.snowplow.tracker.networkconnection.Request;
import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.storage.EventStore;
import com.snowplowanalytics.snowplow.tracker.utils.LogLevel;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.snowplowanalytics.snowplow.tracker.emitter.BufferOption.DefaultGroup;
import static com.snowplowanalytics.snowplow.tracker.emitter.BufferOption.HeavyGroup;
import static com.snowplowanalytics.snowplow.tracker.emitter.BufferOption.Single;
import static com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod.GET;
import static com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod.POST;
import static com.snowplowanalytics.snowplow.tracker.emitter.RequestSecurity.HTTP;
import static com.snowplowanalytics.snowplow.tracker.emitter.RequestSecurity.HTTPS;

public class EmitterTest extends AndroidTestCase {

    // Builder Tests

    public void testHttpMethodSet() {
        Emitter emitter = new Emitter.EmitterBuilder("com.acme", getContext())
                .method(GET)
                .build();
        assertEquals(GET, emitter.getHttpMethod());

        emitter = new Emitter.EmitterBuilder("com.acme", getContext())
                .method(POST)
                .build();
        assertEquals(POST, emitter.getHttpMethod());
    }

    public void testBufferOptionSet() {
        Emitter emitter = new Emitter.EmitterBuilder("com.acme", getContext())
                .option(Single)
                .build();
        assertEquals(Single, emitter.getBufferOption());

        emitter = new Emitter.EmitterBuilder("com.acme", getContext())
                .option(DefaultGroup)
                .build();
        assertEquals(DefaultGroup, emitter.getBufferOption());

        emitter = new Emitter.EmitterBuilder("com.acme", getContext())
                .option(HeavyGroup)
                .build();
        assertEquals(HeavyGroup, emitter.getBufferOption());
    }

    public void testCallbackSet() {
        Emitter emitter = new Emitter.EmitterBuilder("com.acme", getContext()).callback(new RequestCallback() {
            @Override
            public void onSuccess(int successCount) {
            }

            @Override
            public void onFailure(int successCount, int failureCount) {
            }
        }).build();

        assertNotNull(emitter.getRequestCallback());
    }

    public void testUriSet() {
        String uri = "com.acme";

        Emitter emitter = new Emitter.EmitterBuilder(uri, getContext())
                .method(GET)
                .security(HTTP)
                .option(Single)
                .build();
        assertEquals("http://" + uri + "/i", emitter.getEmitterUri());

        emitter = new Emitter.EmitterBuilder(uri, getContext())
                .method(POST)
                .security(HTTP)
                .option(DefaultGroup)
                .build();
        assertEquals("http://" + uri + "/com.snowplowanalytics.snowplow/tp2",
                emitter.getEmitterUri());

        emitter = new Emitter.EmitterBuilder(uri, getContext())
                .method(GET)
                .security(HTTPS)
                .option(DefaultGroup)
                .build();
        assertEquals("https://" + uri + "/i", emitter.getEmitterUri());

        emitter = new Emitter.EmitterBuilder(uri, getContext())
                .method(POST)
                .security(HTTPS)
                .option(DefaultGroup)
                .build();
        assertEquals("https://" + uri + "/com.snowplowanalytics.snowplow/tp2", emitter.getEmitterUri());
    }

    public void testSecuritySet() {
        Emitter emitter = new Emitter.EmitterBuilder("com.acme", getContext())
                .security(HTTP)
                .build();
        assertEquals(HTTP, emitter.getRequestSecurity());

        emitter = new Emitter.EmitterBuilder("com.acme", getContext())
                .security(HTTPS)
                .build();
        assertEquals(RequestSecurity.HTTPS, emitter.getRequestSecurity());
    }

    public void testTickSet() {
        Emitter emitter = new Emitter.EmitterBuilder("com.acme", getContext())
                .tick(0)
                .build();
        assertEquals(0, emitter.getEmitterTick());
    }

    public void testEmptyLimitSet() {
        Emitter emitter = new Emitter.EmitterBuilder("com.acme", getContext())
                .emptyLimit(0)
                .build();
        assertEquals(0, emitter.getEmptyLimit());
    }

    public void testSendLimitSet() {
        Emitter emitter = new Emitter.EmitterBuilder("com.acme", getContext())
                .sendLimit(200)
                .build();
        assertEquals(200, emitter.getSendLimit());
    }

    public void testByteLimitGetSet() {
        Emitter emitter = new Emitter.EmitterBuilder("com.acme", getContext())
                .byteLimitGet(20000)
                .build();
        assertEquals(20000, emitter.getByteLimitGet());
    }

    public void testByteLimitPostSet() {
        Emitter emitter = new Emitter.EmitterBuilder("com.acme", getContext())
                .byteLimitPost(25000)
                .build();
        assertEquals(25000, emitter.getByteLimitPost());
    }

    public void testUpdatingEmitterSettings() throws InterruptedException {
        String uri = "snowplowanalytics.com";
        Emitter emitter = new Emitter.EmitterBuilder(uri, getContext())
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
                .build();

        assertFalse(emitter.getEmitterStatus());
        assertEquals(Single, emitter.getBufferOption());
        assertEquals("http://" + uri + "/com.snowplowanalytics.snowplow/tp2", emitter.getEmitterUri());
        emitter.setHttpMethod(GET);
        assertEquals("http://" + uri + "/i", emitter.getEmitterUri());
        emitter.setRequestSecurity(RequestSecurity.HTTPS);
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
        emitter.setRequestSecurity(HTTP);
        assertEquals("https://com.acme/i", emitter.getEmitterUri());
        emitter.setEmitterUri("com/foo");
        assertEquals("https://com.acme/i", emitter.getEmitterUri());
        emitter.setBufferOption(DefaultGroup);
        assertEquals(HeavyGroup, emitter.getBufferOption());

        emitter.shutdown();

        Emitter customPathEmitter = new Emitter.EmitterBuilder(uri, getContext())
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
                .build();
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

    public void testEmitEventsPostAsGroup() throws InterruptedException {
        MockNetworkConnection networkConnection = new MockNetworkConnection(POST, false);
        Emitter emitter = getEmitter(networkConnection, DefaultGroup);

        List<Payload> payloads = generatePayloads(15);
        for (int i = 0; i < 14; i++) {
            emitter.add(payloads.get(i));
        }

        for (int i = 0; i < 10 && (networkConnection.sendingCount() < 1 || emitter.getEmitterStatus()); i++) {
            Thread.sleep(600);
        }

        assertEquals(14, emitter.getEventStore().getSize());
        networkConnection.successfulConnection = true;
        int prevSendingCount = networkConnection.sendingCount();
        emitter.add(payloads.get(14));

        for (int i = 0; i < 10 && (networkConnection.sendingCount() - prevSendingCount < 1 || emitter.getEmitterStatus()); i++) {
            Thread.sleep(600);
        }

        assertEquals(0, emitter.getEventStore().getSize());
        int totEvents = 0;
        boolean areGrouped = false;
        List<List<RequestResult>> prevResults = networkConnection.previousResults.subList(prevSendingCount, networkConnection.previousResults.size());
        for (List<RequestResult> results : prevResults) {
            for (RequestResult result : results) {
                assertTrue(result.getSuccess());
                int ids = result.getEventIds().size();
                totEvents += ids;
                areGrouped = areGrouped || ids > 1;
            }
        }
        assertEquals(15, totEvents);
        assertTrue(areGrouped);

        emitter.flush();
    }

    public void testEmitOversizeEventsPostAsGroup() throws InterruptedException {
        Logger.updateLogLevel(LogLevel.VERBOSE);

        MockNetworkConnection networkConnection = new MockNetworkConnection(POST, false);
        Emitter emitter = getEmitterBuilder(networkConnection, DefaultGroup)
                .byteLimitPost(5)
                .build();

        List<Payload> payloads = generatePayloads(15);
        for (int i = 0; i < 14; i++) {
            emitter.add(payloads.get(i));
        }

        for (int i = 0; i < 10 && (networkConnection.sendingCount() < 1 || emitter.getEmitterStatus()); i++) {
            Thread.sleep(600);
        }

        assertEquals(0, emitter.getEventStore().getSize());
        networkConnection.successfulConnection = true;
        emitter.add(payloads.get(14));

        for (int i = 0; i < 10 && (networkConnection.sendingCount() < 2 || emitter.getEmitterStatus()); i++) {
            Thread.sleep(600);
        }

        assertEquals(0, emitter.getEventStore().getSize());

        emitter.flush();
    }

    // Emitter Builder

    public Emitter getEmitter(NetworkConnection networkConnection, BufferOption option) {
        return getEmitterBuilder(networkConnection, option).build();
    }

    public Emitter.EmitterBuilder getEmitterBuilder(NetworkConnection networkConnection, BufferOption option) {
        return new Emitter.EmitterBuilder("com.acme", getContext())
                .networkConnection(networkConnection)
                .option(option)
                .tick(0)
                .emptyLimit(0)
                .sendLimit(200)
                .byteLimitGet(20000)
                .byteLimitPost(25000)
                .timeUnit(TimeUnit.SECONDS)
                .tls(EnumSet.of(TLSVersion.TLSv1_2))
                .eventStore(new MockEventStore());
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

class MockEventStore implements EventStore {
    private HashMap<Long, Payload> db = new HashMap<>();
    private long lastInsertedRow = -1;

    @Override
    public void add(@NonNull Payload payload) {
        synchronized (this) {
            lastInsertedRow++;
            Logger.v("MockEventStore", "Add %s", payload);
            db.put(lastInsertedRow, payload);
        }
    }

    @Override
    public boolean removeEvent(long id) {
        synchronized (this) {
            Logger.v("MockEventStore", "Remove %s", id);
            return db.remove(id) != null;
        }
    }

    @Override
    public boolean removeEvents(@NonNull List<Long> ids) {
        boolean result = true;
        for (long id : ids) {
            boolean removed = removeEvent(id);
            result = result && removed;
        }
        return result;
    }

    @Override
    public boolean removeAllEvents() {
        synchronized (this) {
            Logger.v("MockEventStore", "Remove all");
            db = new HashMap<>();
            lastInsertedRow = 0;
        }
        return true;
    }

    @Override
    public long getSize() {
        return db.size();
    }

    @NonNull
    @Override
    public List<EmitterEvent> getEmittableEvents(int queryLimit) {
        synchronized (this) {
            List<Long> eventIds = new ArrayList<>();
            List<EmitterEvent> events = new ArrayList<>();
            for (Map.Entry<Long, Payload> entry : db.entrySet()) {
                Payload payloadCopy = new TrackerPayload();
                payloadCopy.addMap(entry.getValue().getMap());
                EmitterEvent event = new EmitterEvent(payloadCopy, entry.getKey());
                eventIds.add(event.eventId);
                events.add(event);
            }
            if (queryLimit < events.size()) {
                events = events.subList(0, queryLimit);
            }
            Logger.v("MockEventStore", "getEmittableEvents: %s", eventIds);
            return events;
        }
    }
}
