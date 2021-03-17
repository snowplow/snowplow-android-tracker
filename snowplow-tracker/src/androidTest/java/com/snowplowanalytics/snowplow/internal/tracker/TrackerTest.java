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

package com.snowplowanalytics.snowplow.internal.tracker;

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.emitter.EventStore;
import com.snowplowanalytics.snowplow.event.SelfDescribing;
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

import java.io.IOException;
import java.util.Map;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertNotEquals;

public class TrackerTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        try {
            Tracker tracker = Tracker.instance();
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
            Tracker.close();
            Log.i("TrackerTest", "Tracker closed");
        } catch(IllegalStateException e) {
            Log.i("TrackerTest", "Tracker already closed.");
        }
    }

    // Helper Methods
    private Tracker getTracker() {
        return getTracker(false);
    }

    private Tracker getTracker(boolean installTracking) {
        Emitter emitter = new Emitter
                .EmitterBuilder("testUrl", getContext())
                .tick(0)
                .emptyLimit(0)
                .build();

        Subject subject = new Subject
                .SubjectBuilder()
                .context(getContext())
                .build();

        return new Tracker.TrackerBuilder(emitter, "myNamespace", "myAppId", getContext())
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
            .build();
    }

    // Tests

    public void testTrackerNotInit() {
        boolean exception = false;
        try {
            Tracker.instance();
        } catch (Exception e) {
            assertEquals("FATAL: Tracker must be initialized first!", e.getMessage());
            exception = true;
        }
        assertTrue(exception);
    }

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

    public void testEmitterUpdate() {
        Tracker tracker = getTracker();
        assertNotNull(tracker.getEmitter());

        tracker.setEmitter(new Emitter.EmitterBuilder("test", getContext()).build());
        assertNotNull(tracker.getEmitter());
    }

    public void testSubjectUpdate() {
        Tracker tracker = getTracker();
        assertNotNull(tracker.getSubject());

        tracker.setSubject(null);
        assertNull(tracker.getSubject());
    }

    public void testPlatformUpdate() {
        Tracker tracker = getTracker();
        assertEquals(DevicePlatform.InternetOfThings, tracker.getPlatform());

        tracker.setPlatform(DevicePlatform.Mobile);
        assertEquals(DevicePlatform.Mobile, tracker.getPlatform());
    }

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

    public void testTrackEventMultipleTimes() {
        Timing event = Timing.builder()
                .category("category")
                .variable("variable")
                .timing(100)
                .build();
        UUID id1 = new TrackerEvent(event).eventId;
        UUID id2 = new TrackerEvent(event).eventId;
        assertNotEquals(id1, id2);
    }

    public void testTrackSelfDescribingEvent() throws JSONException, IOException, InterruptedException {
        MockWebServer mockWebServer = getMockServer(1);

        Emitter emitter = null;
        try {
            emitter = new Emitter.EmitterBuilder(getMockServerURI(mockWebServer), getContext())
                    .option(BufferOption.Single)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception on Emitter creation");
        }

        Tracker tracker = new Tracker.TrackerBuilder(emitter, "myNamespace", "testTrackWithNoContext", getContext())
                .base64(false)
                .level(LogLevel.VERBOSE)
                .sessionContext(false)
                .mobileContext(false)
                .screenContext(false)
                .geoLocationContext(false)
                .build();

        Log.i("testTrackSelfDescribingEvent", "Send SelfDescribing event");

        SelfDescribingJson sdj = new SelfDescribingJson("iglu:foo/bar/jsonschema/1-0-0");

        SelfDescribing sdEvent = SelfDescribing.builder()
                .eventData(sdj)
                .build();

        tracker.track(sdEvent);
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

        mockWebServer.shutdown();
    }

    public void testTrackWithNoContext() throws Exception {
        Executor.setThreadCount(30);
        Executor.shutdown();

        MockWebServer mockWebServer = getMockServer(1);

        Emitter emitter = null;
        try {
            emitter = new Emitter.EmitterBuilder(getMockServerURI(mockWebServer), getContext())
                    .option(BufferOption.Single)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception on Emitter creation");
        }

        Tracker tracker = new Tracker.TrackerBuilder(emitter, "myNamespace", "testTrackWithNoContext", getContext())
                .base64(false)
                .level(LogLevel.VERBOSE)
                .sessionContext(false)
                .mobileContext(false)
                .screenContext(false)
                .geoLocationContext(false)
                .build();

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

    public void testTrackWithoutDataCollection() throws Exception {
        MockWebServer mockWebServer = getMockServer(1);

        Emitter emitter = new Emitter.EmitterBuilder(getMockServerURI(mockWebServer), getContext())
                .option(BufferOption.Single)
                .build();

        Tracker tracker = new Tracker.TrackerBuilder(emitter, "myNamespace", "myAppId", getContext())
            .base64(false)
            .level(LogLevel.VERBOSE)
            .sessionContext(false)
            .mobileContext(false)
            .geoLocationContext(false)
            .build();

        tracker.pauseEventTracking();
        tracker.track(new ScreenView("name"));
        RecordedRequest req = mockWebServer.takeRequest(2, TimeUnit.SECONDS);

        assertEquals(0, tracker.getEmitter().getEventStore().getSize());
        assertNull(req);

        mockWebServer.shutdown();
    }

    public void testTrackWithSession() throws Exception {
        MockWebServer mockWebServer = getMockServer(1);

        Emitter emitter = new Emitter.EmitterBuilder(getMockServerURI(mockWebServer), getContext())
                .option(BufferOption.Single)
                .build();

        Tracker tracker = new Tracker.TrackerBuilder(emitter, "myNamespace", "myAppId", getContext())
            .base64(false)
            .level(LogLevel.VERBOSE)
            .sessionContext(true)
            .mobileContext(false)
            .geoLocationContext(false)
            .foregroundTimeout(5)
            .backgroundTimeout(5)
            .timeUnit(TimeUnit.SECONDS)
            .build();

        assertNotNull(tracker.getSession());
        tracker.resumeSessionChecking();
        Thread.sleep(2000);
        tracker.pauseSessionChecking();

        mockWebServer.shutdown();
    }

    public void testTrackScreenView() {
        Emitter emitter = new Emitter.EmitterBuilder("fake-uri", getContext())
                .option(BufferOption.Single)
                .build();

        Tracker tracker = new Tracker.TrackerBuilder(emitter, "myNamespace", "myAppId", getContext())
                .base64(false)
                .level(LogLevel.VERBOSE)
                .sessionContext(false)
                .mobileContext(false)
                .geoLocationContext(false)
                .screenContext(true)
                .foregroundTimeout(5)
                .backgroundTimeout(5)
                .timeUnit(TimeUnit.SECONDS)
                .build();

        ScreenState screenState = tracker.getScreenState();
        assertNotNull(screenState);

        Map<String, Object> screenStateMapWrapper = screenState.getCurrentScreen(true).getMap();
        Map<String, Object> screenStateMap = (Map<String, Object>)screenStateMapWrapper.get(Parameters.DATA);
        assertEquals("Unknown", screenStateMap.get(Parameters.SCREEN_NAME));

        // Send screenView
        ScreenView screenView = ScreenView.builder().name("screen1").build();
        String screenId = (String)screenView.getDataPayload().get("id");
        tracker.track(screenView);

        screenStateMapWrapper = screenState.getCurrentScreen(true).getMap();
        screenStateMap = (Map<String, Object>)screenStateMapWrapper.get(Parameters.DATA);
        assertEquals("screen1", screenStateMap.get(Parameters.SCREEN_NAME));
        assertEquals(screenId, screenStateMap.get(Parameters.SCREEN_ID));

        // Send another screenView
        screenView = ScreenView.builder().name("screen2").build();
        String screenId1 = (String)screenView.getDataPayload().get("id");
        tracker.track(screenView);

        Map<String, Object> payload = (Map<String, Object>)screenView.getDataPayload();
        assertEquals("screen2", payload.get(Parameters.SCREEN_NAME));
        assertEquals(screenId1, payload.get(Parameters.SCREEN_ID));
        assertEquals("screen1", payload.get(Parameters.SV_PREVIOUS_NAME));
        assertEquals(screenId, payload.get(Parameters.SV_PREVIOUS_ID));
    }

    public void testTrackUncaughtException() {
        Thread.setDefaultUncaughtExceptionHandler(
                new TestExceptionHandler("Illegal State Exception has been thrown!")
        );

        assertEquals(
                TestExceptionHandler.class,
                Thread.getDefaultUncaughtExceptionHandler().getClass()
        );

        Emitter emitter = new Emitter.EmitterBuilder("com.acme", getContext()).build();

        Tracker tracker = new Tracker.TrackerBuilder(emitter, "myNamespace", "myAppId", getContext())
                .base64(false)
                .level(LogLevel.VERBOSE)
                .applicationCrash(true)
                .build();

        assertTrue(tracker.getApplicationCrash());
        assertEquals(
                ExceptionHandler.class,
                Thread.getDefaultUncaughtExceptionHandler().getClass()
        );
    }

    public void testExceptionHandler() {
        TestExceptionHandler handler = new TestExceptionHandler("Illegal State Exception has been thrown!");
        Thread.setDefaultUncaughtExceptionHandler(handler);

        assertEquals(
                TestExceptionHandler.class,
                Thread.getDefaultUncaughtExceptionHandler().getClass()
        );

        Emitter emitter = new Emitter.EmitterBuilder("com.acme", getContext()).build();
        new Tracker.TrackerBuilder(emitter, "myNamespace", "myAppId", getContext())
                .base64(false)
                .level(LogLevel.VERBOSE)
                .applicationCrash(false)
                .build();

        ExceptionHandler handler1 = new ExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(handler1);

        assertEquals(
                ExceptionHandler.class,
                Thread.getDefaultUncaughtExceptionHandler().getClass()
        );

        handler1.uncaughtException(Thread.currentThread(), new Throwable("Illegal State Exception has been thrown!"));
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
