/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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

import com.snowplowanalytics.core.emitter.Emitter;
import com.snowplowanalytics.snowplow.network.NetworkConnection;
import com.snowplowanalytics.snowplow.emitter.BufferOption;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.Protocol;
import com.snowplowanalytics.snowplow.network.RequestCallback;
import com.snowplowanalytics.core.emitter.TLSVersion;
import com.snowplowanalytics.snowplow.network.Request;
import com.snowplowanalytics.snowplow.payload.Payload;
import com.snowplowanalytics.snowplow.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.network.RequestResult;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
        Consumer<Emitter> builder = (emitter -> emitter.setHttpMethod(GET));
        Emitter emitter = new Emitter(getContext(), "com.acme", builder);
        assertEquals(GET, emitter.getHttpMethod());

        builder = (emitter1 -> emitter1.setHttpMethod(POST));
        emitter = new Emitter(getContext(), "com.acme", builder);
        assertEquals(POST, emitter.getHttpMethod());
    }

    public void testBufferOptionSet() {
        Consumer<Emitter> builder = (emitter -> emitter.setBufferOption(Single));
        Emitter emitter = new Emitter(getContext(), "com.acme", builder);
        assertEquals(Single, emitter.getBufferOption());

        builder = (emitter1 -> emitter1.setBufferOption(Single));
        emitter = new Emitter(getContext(), "com.acme", builder);
        assertEquals(DefaultGroup, emitter.getBufferOption());

        builder = (emitter2 -> emitter2.setBufferOption(Single));
        emitter = new Emitter(getContext(), "com.acme", builder);
        assertEquals(HeavyGroup, emitter.getBufferOption());
    }

    public void testCallbackSet() {
        Consumer<Emitter> builder = (emitter -> emitter.setRequestCallback(
                new RequestCallback() {
                    @Override
                    public void onSuccess(int successCount) {
                    }

                    @Override
                    public void onFailure(int successCount, int failureCount) {
                    }
                }
        ));
        Emitter emitter = new Emitter(getContext(), "com.acme", builder);

        assertNotNull(emitter.getRequestCallback());
    }

    public void testUriSet() {
        String uri = "com.acme";

        Consumer<Emitter> builder = (emitter -> {
            emitter.setBufferOption(Single);
            emitter.setHttpMethod(GET);
            emitter.setRequestSecurity(HTTP);
        });
        Emitter emitter = new Emitter(getContext(), uri, builder);
        assertEquals("http://" + uri + "/i", emitter.getEmitterUri());

        builder = (emitter1 -> {
            emitter1.setBufferOption(DefaultGroup);
            emitter1.setHttpMethod(POST);
            emitter1.setRequestSecurity(HTTP);
        });
        emitter = new Emitter(getContext(), uri, builder);
        assertEquals("http://" + uri + "/com.snowplowanalytics.snowplow/tp2",
                emitter.getEmitterUri());

        builder = (emitter2 -> {
            emitter2.setBufferOption(DefaultGroup);
            emitter2.setHttpMethod(GET);
            emitter2.setRequestSecurity(HTTPS);
        });
        emitter = new Emitter(getContext(), uri, builder);
        assertEquals("https://" + uri + "/i", emitter.getEmitterUri());

        builder = (emitter3 -> {
            emitter3.setBufferOption(DefaultGroup);
            emitter3.setHttpMethod(POST);
            emitter3.setRequestSecurity(HTTPS);
        });
        emitter = new Emitter(getContext(), uri, builder);
        assertEquals("https://" + uri + "/com.snowplowanalytics.snowplow/tp2", emitter.getEmitterUri());
    }

    public void testSecuritySet() {
        Consumer<Emitter> builder = (emitter -> emitter.setRequestSecurity(HTTP));
        Emitter emitter = new Emitter(getContext(), "com.acme", builder);
        assertEquals(HTTP, emitter.getRequestSecurity());

        builder = (emitter1 -> emitter1.setRequestSecurity(HTTPS));
        emitter = new Emitter(getContext(), "com.acme", builder);
        assertEquals(Protocol.HTTPS, emitter.getRequestSecurity());
    }

    public void testTickSet() {
        Consumer<Emitter> builder = (emitter -> emitter.setEmitterTick(0));
        Emitter emitter = new Emitter(getContext(), "com.acme", builder);
        assertEquals(0, emitter.getEmitterTick());
    }

    public void testEmptyLimitSet() {
        Consumer<Emitter> builder = (emitter -> emitter.setEmptyLimit(0));
        Emitter emitter = new Emitter(getContext(), "com.acme", builder);
        assertEquals(0, emitter.getEmptyLimit());
    }

    public void testSendLimitSet() {
        Consumer<Emitter> builder = (emitter -> emitter.setSendLimit(200));
        Emitter emitter = new Emitter(getContext(), "com.acme", builder);
        assertEquals(200, emitter.getSendLimit());
    }

    public void testByteLimitGetSet() {
        Consumer<Emitter> builder = (emitter -> emitter.setByteLimitGet(20000));
        Emitter emitter = new Emitter(getContext(), "com.acme", builder);
        assertEquals(20000, emitter.getByteLimitGet());
    }

    public void testByteLimitPostSet() {
        Consumer<Emitter> builder = (emitter -> emitter.setByteLimitPost(25000));
        Emitter emitter = new Emitter(getContext(), "com.acme", builder);
        assertEquals(25000, emitter.getByteLimitPost());
    }

    public void testUpdatingEmitterSettings() throws InterruptedException {
        String uri = "snowplow.io";
        Consumer<Emitter> builder = (emitter -> {
            emitter.setBufferOption(Single);
            emitter.setHttpMethod(POST);
            emitter.setRequestSecurity(HTTP);
            emitter.setEmitterTick(250);
            emitter.setEmptyLimit(5);
            emitter.setSendLimit(200);
            emitter.setByteLimitGet(20000);
            emitter.setByteLimitPost(25000);
            emitter.setEventStore(new MockEventStore());
            emitter.setTimeUnit(TimeUnit.MILLISECONDS);
        });
        Emitter emitter = new Emitter(getContext(), uri, builder);

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
        assertEquals("https://com.acme/com.snowplowanalytics.snowplow/tp2", emitter.getEmitterUri());
        emitter.setEmitterUri("com.foo");
        assertEquals("https://com.foo/com.snowplowanalytics.snowplow/tp2", emitter.getEmitterUri());
        emitter.setBufferOption(DefaultGroup);
        assertEquals(HeavyGroup, emitter.getBufferOption());

        emitter.shutdown();

        builder = (emitter1 -> {
            emitter1.setBufferOption(Single);
            emitter1.setHttpMethod(POST);
            emitter1.setRequestSecurity(HTTP);
            emitter1.setEmitterTick(250);
            emitter1.setEmptyLimit(5);
            emitter1.setSendLimit(200);
            emitter1.setByteLimitGet(20000);
            emitter1.setByteLimitPost(25000);
            emitter1.setEventStore(new MockEventStore());
            emitter1.setTimeUnit(TimeUnit.MILLISECONDS);
            emitter1.setCustomPostPath("com.acme.company/tpx");
        });
        Emitter customPathEmitter = new Emitter(getContext(), uri, builder);
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
        MockNetworkConnection networkConnection = new MockNetworkConnection(GET,200);
        Emitter emitter = getEmitter(networkConnection, Single);

        emitter.add(generatePayloads(1).get(0));

        for (int i = 0; i < 10 && (networkConnection.sendingCount() < 1 || emitter.getEmitterStatus()); i++) {
            Thread.sleep(600);
        }

        assertEquals(1, networkConnection.previousResults.size());
        assertEquals(1, networkConnection.previousResults.get(0).size());
        assertTrue(networkConnection.previousResults.get(0).get(0).isSuccessful());
        assertEquals(0, emitter.getEventStore().getSize());

        emitter.flush();
    }

    public void testEmitSingleGetEventWithNoSuccess() throws InterruptedException {
        MockNetworkConnection networkConnection = new MockNetworkConnection(GET,500);
        Emitter emitter = getEmitter(networkConnection, Single);

        emitter.add(generatePayloads(1).get(0));

        for (int i = 0; i < 10 && (networkConnection.sendingCount() < 1 || emitter.getEmitterStatus()); i++) {
            Thread.sleep(600);
        }

        assertEquals(1, networkConnection.previousResults.size());
        assertEquals(1, networkConnection.previousResults.get(0).size());
        assertFalse(networkConnection.previousResults.get(0).get(0).isSuccessful());
        assertEquals(1, emitter.getEventStore().getSize());

        emitter.flush();
    }

    public void testEmitTwoGetEventsWithSuccess() throws InterruptedException {
        MockNetworkConnection networkConnection = new MockNetworkConnection(GET,200);
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
                assertTrue(result.isSuccessful());
                totEvents += result.getEventIds().size();
            }
        }
        assertEquals(2, totEvents);

        emitter.flush();
    }

    public void testEmitTwoGetEventsWithNoSuccess() throws InterruptedException {
        MockNetworkConnection networkConnection = new MockNetworkConnection(GET,500);
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
                assertFalse(result.isSuccessful());
            }
        }
        // Can't check the total number of events sent as the Emitter stops to send if a request fails.

        emitter.flush();
    }

    public void testEmitSinglePostEventWithSuccess() throws InterruptedException {
        MockNetworkConnection networkConnection = new MockNetworkConnection(POST, 200);
        Emitter emitter = getEmitter(networkConnection, Single);
        emitter.add(generatePayloads(1).get(0));

        for (int i = 0; i < 10 && networkConnection.sendingCount() < 1; i++) {
            Thread.sleep(600);
        }

        assertEquals(1, networkConnection.previousResults.size());
        assertEquals(1, networkConnection.previousResults.get(0).size());
        assertTrue(networkConnection.previousResults.get(0).get(0).isSuccessful());
        assertEquals(0, emitter.getEventStore().getSize());

        emitter.flush();
    }

    public void testPauseAndResumeEmittingEvents() throws InterruptedException {
        MockNetworkConnection networkConnection = new MockNetworkConnection(POST, 200);
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
        assertTrue(networkConnection.previousResults.get(0).get(0).isSuccessful());
        assertEquals(0, emitter.getEventStore().getSize());

        emitter.flush();
    }

    public void testUpdatesNetworkConnectionWhileRunning() throws InterruptedException {
        Consumer<Emitter> builder = (emitter -> emitter.setEventStore(new MockEventStore()));
        Emitter emitter = new Emitter(getContext(), "com.acme", builder);

        emitter.flush();
        Thread.sleep(100);
        assertTrue(emitter.getEmitterStatus()); // is running
        emitter.setEmitterUri("new.uri"); // update while running
        assertTrue(emitter.getEmitterStatus()); // is running
        assertTrue(emitter.getEmitterUri().contains("new.uri"));
    }

    public void testRemovesEventsFromQueueOnNoRetryStatus() throws InterruptedException {
        MockNetworkConnection networkConnection = new MockNetworkConnection(GET,403);
        Emitter emitter = getEmitter(networkConnection, Single);

        emitter.add(generatePayloads(1).get(0));

        for (int i = 0; i < 10 && (networkConnection.sendingCount() < 1 || emitter.getEmitterStatus()); i++) {
            Thread.sleep(600);
        }

        assertEquals(1, networkConnection.previousResults.size());
        assertFalse(networkConnection.previousResults.get(0).get(0).isSuccessful());
        assertEquals(0, emitter.getEventStore().getSize());

        emitter.flush();
    }

    public void testFollowCustomRetryRules() throws InterruptedException {
        MockNetworkConnection networkConnection = new MockNetworkConnection(GET,500);
        Emitter emitter = getEmitter(networkConnection, Single);

        Map<Integer, Boolean> customRules = new HashMap<>();
        customRules.put(403, true);
        customRules.put(500, false);
        emitter.setCustomRetryForStatusCodes(customRules);

        // no events in queue since they were dropped because retrying is disabled for 500
        emitter.add(generatePayloads(1).get(0));

        Thread.sleep(1000);

        networkConnection.statusCode = 403;

        emitter.add(generatePayloads(1).get(0));

        Thread.sleep(1000);

        // event still in queue because retrying is enabled for 403
        assertEquals(1, emitter.getEventStore().getSize());

        emitter.flush();
    }

    // Emitter Builder

    public Emitter getEmitter(NetworkConnection networkConnection, BufferOption option) {
        Consumer<Emitter> builder = (emitter -> {
            emitter.setNetworkConnection(networkConnection);
            
            emitter.setBufferOption(option);
            emitter.setEmitterTick(0);
            emitter.setEmptyLimit(0);
            emitter.setSendLimit(200);
            emitter.setByteLimitGet(20000);
            emitter.setByteLimitPost(25000);
            emitter.setEventStore(new MockEventStore());
            emitter.setTimeUnit(TimeUnit.SECONDS);
            emitter.setTlsVersions(EnumSet.of(TLSVersion.TLSv1_2));
            
        });
        return new Emitter(getContext(), "com.acme", builder);
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
