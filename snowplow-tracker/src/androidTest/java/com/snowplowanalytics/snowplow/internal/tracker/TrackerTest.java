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

package com.snowplowanalytics.snowplow.internal.tracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snowplowanalytics.snowplow.TestUtils;
import com.snowplowanalytics.snowplow.emitter.EventStore;
import com.snowplowanalytics.snowplow.event.SelfDescribing;
import com.snowplowanalytics.snowplow.event.Structured;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.DevicePlatform;
import com.snowplowanalytics.snowplow.internal.emitter.Emitter;
import com.snowplowanalytics.snowplow.internal.emitter.Executor;
import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.emitter.BufferOption;
import com.snowplowanalytics.snowplow.event.ScreenView;
import com.snowplowanalytics.snowplow.event.Timing;
import com.snowplowanalytics.snowplow.tracker.LogLevel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Map;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class TrackerTest {

    private static Tracker tracker;

    @Before
    public synchronized void setUp() throws Exception {
        try {
            if (tracker == null) return;
            Emitter emitter = tracker.getEmitter();
            EventStore eventStore = emitter.getEventStore();
            if (eventStore != null) {
                boolean isClean = eventStore.removeAllEvents();
                Log.i("TrackerTest", "EventStore cleaned: " + isClean);
                Log.i("TrackerTest", "Events in the store: " + eventStore.getSize());
            } else {
                Log.i("TrackerTest", "EventStore null");
            }
            emitter.shutdown(30);
            tracker.close();
            Log.i("TrackerTest", "Tracker closed");
        } catch(IllegalStateException e) {
            Log.i("TrackerTest", "Tracker already closed.");
        }
    }

    // Helper Methods
    private Tracker getTracker() {
        return getTracker(false);
    }

    private synchronized Tracker getTracker(boolean installTracking) {
        String namespace = "myNamespace";
        TestUtils.createSessionSharedPreferences(getContext(), namespace);

        Emitter emitter = new Emitter(getContext(), "testUrl", new Emitter.EmitterBuilder()
                .tick(0)
                .emptyLimit(0)
        );

        Subject subject = new Subject(getContext(), null);

        tracker = new Tracker(new Tracker.TrackerBuilder(emitter, "myNamespace", "myAppId", getContext())
            .subject(subject)
            .platform(DevicePlatform.InternetOfThings)
            .base64(false)
            .level(LogLevel.VERBOSE)
            .threadCount(1)
            .sessionContext(false)
            .mobileContext(false)
            .geoLocationContext(false)
            .foregroundTimeout(5)
            .backgroundTimeout(5)
            .timeUnit(TimeUnit.SECONDS)
            .applicationCrash(false)
            .lifecycleEvents(true)
            .installTracking(installTracking)
            .applicationContext(true)
        );
        return tracker;
    }

    private Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    // Tests

    @Test
    public void testSetValues() {
        Tracker tracker = getTracker(true);
        assertEquals("myNamespace", tracker.getNamespace());
        assertEquals("myAppId", tracker.getAppId());
        assertEquals(DevicePlatform.InternetOfThings, tracker.getPlatform());
        assertFalse(tracker.getBase64Encoded());
        assertNotNull(tracker.getEmitter());
        assertNotNull(tracker.getSubject());
        assertEquals(LogLevel.VERBOSE, tracker.getLogLevel());
        assertEquals(2, tracker.getThreadCount());
        assertFalse(tracker.getApplicationCrash());
        assertTrue(tracker.getLifecycleEvents());
        assertTrue(tracker.getInstallTracking());
        assertTrue(tracker.getApplicationContext());
    }

    @Test
    public void testEmitterUpdate() {
        Tracker tracker = getTracker();
        assertNotNull(tracker.getEmitter());

        tracker.setEmitter(new Emitter(getContext(), "test", null));
        assertNotNull(tracker.getEmitter());
    }

    @Test
    public void testSubjectUpdate() {
        Tracker tracker = getTracker();
        assertNotNull(tracker.getSubject());

        tracker.setSubject(null);
        assertNull(tracker.getSubject());
    }

    @Test
    public void testPlatformUpdate() {
        Tracker tracker = getTracker();
        assertEquals(DevicePlatform.InternetOfThings, tracker.getPlatform());

        tracker.setPlatform(DevicePlatform.Mobile);
        assertEquals(DevicePlatform.Mobile, tracker.getPlatform());
    }

    @Test
    public void testDataCollectionSwitch() {
        Tracker tracker = getTracker();
        assertTrue(tracker.getDataCollection());

        tracker.pauseEventTracking();
        assertFalse(tracker.getDataCollection());
        tracker.pauseEventTracking();
        assertFalse(tracker.getDataCollection());

        tracker.resumeEventTracking();
        assertTrue(tracker.getDataCollection());
        tracker.resumeEventTracking();
        assertTrue(tracker.getDataCollection());
    }

    @Test
    public void testTrackEventMultipleTimes() {
        Timing event = new Timing("category", "variable", 100);
        UUID id1 = new TrackerEvent(event).eventId;
        UUID id2 = new TrackerEvent(event).eventId;
        assertNotEquals(id1, id2);
    }

    @Test
    public void testTrackSelfDescribingEvent() throws JSONException, IOException, InterruptedException {
        Executor.setThreadCount(30);
        Executor.shutdown();

        String namespace = "myNamespace";
        TestUtils.createSessionSharedPreferences(getContext(), namespace);

        MockWebServer mockWebServer = getMockServer(1);

        Emitter emitter = null;
        try {
            emitter = new Emitter(getContext(), getMockServerURI(mockWebServer), new Emitter.EmitterBuilder()
                    .option(BufferOption.Single)
            );
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception on Emitter creation");
        }

        tracker = new Tracker(new Tracker.TrackerBuilder(emitter, namespace, "testTrackWithNoContext", getContext())
                .base64(false)
                .level(LogLevel.VERBOSE)
                .sessionContext(false)
                .mobileContext(false)
                .screenContext(false)
                .geoLocationContext(false)
                .installTracking(false)
                .applicationCrash(false)
                .screenviewEvents(false)
        );

        EventStore eventStore = emitter.getEventStore();
        if (eventStore != null) {
            boolean isClean = eventStore.removeAllEvents();
            Log.i("testTrackSelfDescribingEvent", "EventStore clean: " + isClean);
        }

        Log.i("testTrackSelfDescribingEvent", "Send SelfDescribing event");

        SelfDescribingJson sdj = new SelfDescribingJson("iglu:foo/bar/jsonschema/1-0-0");

        SelfDescribing sdEvent = new SelfDescribing(sdj);

        UUID eventId = tracker.track(sdEvent);
        RecordedRequest req = mockWebServer.takeRequest(60, TimeUnit.SECONDS);
        assertNotNull(req);
        int reqCount = mockWebServer.getRequestCount();
        assertEquals(1, reqCount);

        JSONObject payload = new JSONObject(req.getBody().readUtf8());
        assertEquals(2, payload.length());
        assertEquals(
                "iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-4",
                payload.getString("schema")
        );
        JSONArray data = payload.getJSONArray("data");
        assertEquals(1, data.length());
        JSONObject event = data.getJSONObject(0);

        assertEquals("ue", event.getString(Parameters.EVENT));
        assertFalse(event.has(Parameters.UNSTRUCTURED_ENCODED));
        assertEquals(eventId.toString(), event.getString(Parameters.EID));

        mockWebServer.shutdown();
    }

    @Test
    public void testTrackWithNoContext() throws Exception {
        Executor.setThreadCount(30);
        Executor.shutdown();

        String namespace = "myNamespace";
        TestUtils.createSessionSharedPreferences(getContext(), namespace);

        MockWebServer mockWebServer = getMockServer(1);

        Emitter emitter = null;
        try {
            emitter = new Emitter(getContext(), getMockServerURI(mockWebServer), new Emitter.EmitterBuilder()
                    .option(BufferOption.Single)
            );
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception on Emitter creation");
        }

        tracker = new Tracker(new Tracker.TrackerBuilder(emitter, namespace, "testTrackWithNoContext", getContext())
                .base64(false)
                .level(LogLevel.VERBOSE)
                .sessionContext(false)
                .mobileContext(false)
                .screenContext(false)
                .geoLocationContext(false)
                .installTracking(false)
                .applicationCrash(false)
                .screenviewEvents(false)
        );

        Log.i("testTrackWithNoContext", "Send ScreenView event");
        tracker.track(new ScreenView("name"));
        RecordedRequest req = mockWebServer.takeRequest(60, TimeUnit.SECONDS);
        assertNotNull(req);
        int reqCount = mockWebServer.getRequestCount();
        assertEquals(1, reqCount);

        JSONObject payload = new JSONObject(req.getBody().readUtf8());
        assertEquals(2, payload.length());
        assertEquals(
                "iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-4",
                payload.getString("schema")
        );

        JSONArray data = payload.getJSONArray("data");
        assertEquals(1, data.length());
        JSONObject event = data.getJSONObject(0);

        boolean found = true;
        try {
            String co = event.getString(Parameters.CONTEXT);
            Log.e("testTrackWithNoContext", "Unexpected event: " + event.toString());
            fail(co);
        } catch (Exception e) {
            found = false;
        }
        assertFalse(found);

        mockWebServer.shutdown();
    }

    @Test
    public void testTrackWithoutDataCollection() throws Exception {
        Executor.setThreadCount(30);
        Executor.shutdown();

        String namespace = "myNamespace";
        TestUtils.createSessionSharedPreferences(getContext(), namespace);

        MockWebServer mockWebServer = getMockServer(1);

        Emitter emitter = new Emitter(getContext(), getMockServerURI(mockWebServer), new Emitter.EmitterBuilder()
                .option(BufferOption.Single)
        );

        tracker = new Tracker(new Tracker.TrackerBuilder(emitter, namespace, "myAppId", getContext())
                .base64(false)
                .level(LogLevel.VERBOSE)
                .sessionContext(false)
                .mobileContext(false)
                .geoLocationContext(false)
                .installTracking(false)
                .applicationCrash(false)
                .screenviewEvents(false)
        );

        tracker.pauseEventTracking();
        UUID eventId = tracker.track(new ScreenView("name"));
        assertNull(eventId);
        RecordedRequest req = mockWebServer.takeRequest(2, TimeUnit.SECONDS);

        assertEquals(0, tracker.getEmitter().getEventStore().getSize());
        assertNull(req);

        mockWebServer.shutdown();
    }

    @Test
    public void testTrackWithSession() throws Exception {
        Executor.setThreadCount(30);
        Executor.shutdown();

        String namespace = "myNamespace";
        TestUtils.createSessionSharedPreferences(getContext(), namespace);

        MockWebServer mockWebServer = getMockServer(1);

        Emitter emitter = new Emitter(getContext(), getMockServerURI(mockWebServer), new Emitter.EmitterBuilder()
                .option(BufferOption.Single)
        );

        tracker = new Tracker(new Tracker.TrackerBuilder(emitter, namespace, "myAppId", getContext())
                .base64(false)
                .level(LogLevel.VERBOSE)
                .sessionContext(true)
                .mobileContext(false)
                .geoLocationContext(false)
                .installTracking(false)
                .applicationCrash(false)
                .screenviewEvents(false)
                .foregroundTimeout(5)
                .backgroundTimeout(5)
                .timeUnit(TimeUnit.SECONDS)
        );

        assertNotNull(tracker.getSession());
        tracker.resumeSessionChecking();
        Thread.sleep(2000);
        tracker.pauseSessionChecking();

        mockWebServer.shutdown();
    }

    @Test
    public void testTrackScreenView() {
        String namespace = "myNamespace";
        TestUtils.createSessionSharedPreferences(getContext(), namespace);

        Emitter emitter = new Emitter(getContext(), "fake-uri", new Emitter.EmitterBuilder()
                .option(BufferOption.Single)
        );

        tracker = new Tracker(new Tracker.TrackerBuilder(emitter, namespace, "myAppId", getContext())
                .base64(false)
                .level(LogLevel.VERBOSE)
                .sessionContext(false)
                .mobileContext(false)
                .geoLocationContext(false)
                .screenContext(true)
                .installTracking(false)
                .applicationCrash(false)
                .screenviewEvents(false)
                .foregroundTimeout(5)
                .backgroundTimeout(5)
                .timeUnit(TimeUnit.SECONDS)
        );

        ScreenState screenState = tracker.getScreenState();
        assertNotNull(screenState);

        Map<String, Object> screenStateMapWrapper = screenState.getCurrentScreen(true).getMap();
        Map<String, Object> screenStateMap = (Map<String, Object>) screenStateMapWrapper.get(Parameters.DATA);
        assertEquals("Unknown", screenStateMap.get(Parameters.SCREEN_NAME));

        // Send screenView
        ScreenView screenView = new ScreenView("screen1");
        String screenId = (String) screenView.getDataPayload().get("id");
        UUID eventId1 = tracker.track(screenView);

        screenStateMapWrapper = tracker.getScreenState().getCurrentScreen(true).getMap();
        screenStateMap = (Map<String, Object>) screenStateMapWrapper.get(Parameters.DATA);
        assertEquals("screen1", screenStateMap.get(Parameters.SCREEN_NAME));
        assertEquals(screenId, screenStateMap.get(Parameters.SCREEN_ID));

        // Send another screenView
        screenView = new ScreenView("screen2");
        String screenId1 = (String) screenView.getDataPayload().get("id");
        UUID eventId2 = tracker.track(screenView);

        assertNotEquals(eventId1.toString(), eventId2.toString());
    }

    @Test
    public void testTrackUncaughtException() {
        String namespace = "myNamespace";
        TestUtils.createSessionSharedPreferences(getContext(), namespace);

        Thread.setDefaultUncaughtExceptionHandler(
                new TestExceptionHandler("Illegal State Exception has been thrown!")
        );

        assertEquals(
                TestExceptionHandler.class,
                Thread.getDefaultUncaughtExceptionHandler().getClass()
        );

        Emitter emitter = new Emitter(getContext(), "com.acme", null);

        tracker = new Tracker(new Tracker.TrackerBuilder(emitter, namespace, "myAppId", getContext())
                .base64(false)
                .level(LogLevel.VERBOSE)
                .installTracking(false)
                .screenviewEvents(false)
                .applicationCrash(true)
        );

        assertTrue(tracker.getApplicationCrash());
        assertEquals(
                ExceptionHandler.class,
                Thread.getDefaultUncaughtExceptionHandler().getClass()
        );
    }

    @Test
    public void testExceptionHandler() {
        String namespace = "myNamespace";
        TestUtils.createSessionSharedPreferences(getContext(), namespace);

        TestExceptionHandler handler = new TestExceptionHandler("Illegal State Exception has been thrown!");
        Thread.setDefaultUncaughtExceptionHandler(handler);

        assertEquals(
                TestExceptionHandler.class,
                Thread.getDefaultUncaughtExceptionHandler().getClass()
        );

        Emitter emitter = new Emitter(getContext(), "com.acme", null);
        tracker = new Tracker(new Tracker.TrackerBuilder(emitter, namespace, "myAppId", getContext())
                .base64(false)
                .level(LogLevel.VERBOSE)
                .installTracking(false)
                .screenviewEvents(false)
                .applicationCrash(false)
        );

        ExceptionHandler handler1 = new ExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(handler1);

        assertEquals(
                ExceptionHandler.class,
                Thread.getDefaultUncaughtExceptionHandler().getClass()
        );

        handler1.uncaughtException(Thread.currentThread(), new Throwable("Illegal State Exception has been thrown!"));
    }

    @Test
    public void testStartsNewSessionWhenChangingAnonymousTracking() {
        Emitter emitter = new Emitter(getContext(), "fake-uri", new Emitter.EmitterBuilder()
                .option(BufferOption.Single)
        );
        emitter.pauseEmit();

        tracker = new Tracker(new Tracker.TrackerBuilder(emitter, "ns", "myAppId", getContext())
                .base64(false)
                .level(LogLevel.VERBOSE)
                .sessionContext(true)
                .installTracking(false)
                .applicationCrash(false)
                .screenviewEvents(false)
                .foregroundTimeout(5)
                .backgroundTimeout(5)
                .timeUnit(TimeUnit.SECONDS)
        );

        tracker.track(new Structured("c", "a"));
        String sessionIdStart = tracker.getSession().getState().getSessionId();

        tracker.setUserAnonymisation(true);
        tracker.track(new Structured("c", "a"));
        String sessionIdAnonymous = tracker.getSession().getState().getSessionId();
        assertNotEquals(sessionIdStart, sessionIdAnonymous);

        tracker.setUserAnonymisation(false);
        tracker.track(new Structured("c", "a"));
        String sessionIdNotAnonymous = tracker.getSession().getState().getSessionId();
        assertNotEquals(sessionIdAnonymous, sessionIdNotAnonymous);
    }

    public static class TestExceptionHandler implements Thread.UncaughtExceptionHandler {

        private final String expectedMessage;

        public TestExceptionHandler(String message) {
            this.expectedMessage = message;
        }

        public void uncaughtException(@NonNull Thread t, Throwable e) {
            assertEquals(this.expectedMessage, e.getMessage());
        }
    }

    // Mock Server

    public MockWebServer getMockServer(int count) throws IOException {
        MockWebServer mockServer = new MockWebServer();
        mockServer.start();
        MockResponse mockResponse = new MockResponse().setResponseCode(200);
        for (int i = 0; i < count; i++) {
            mockServer.enqueue(mockResponse);
        }
        return mockServer;
    }

    @Nullable
    @SuppressLint("DefaultLocale")
    public String getMockServerURI(MockWebServer mockServer) {
        if (mockServer != null) {
            return String.format("%s:%d", mockServer.getHostName(), mockServer.getPort());
        }
        return null;
    }
}
