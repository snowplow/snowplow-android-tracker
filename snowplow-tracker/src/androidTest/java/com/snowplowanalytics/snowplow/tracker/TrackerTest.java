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

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;
import android.util.Log;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.events.ScreenView;
import com.snowplowanalytics.snowplow.tracker.events.Timing;
import com.snowplowanalytics.snowplow.tracker.tracker.ExceptionHandler;
import com.snowplowanalytics.snowplow.tracker.tracker.ScreenState;
import com.snowplowanalytics.snowplow.tracker.utils.LogLevel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class TrackerTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        try {
            Tracker tracker = Tracker.instance();
            Emitter emitter = tracker.getEmitter();
            emitter.shutdown(30);
            tracker.close();
            emitter.waitForEventStore();
            boolean isClean = emitter.getEventStore().removeAllEvents();
            Log.i("TrackerTest", "Tracker closed - EventStore cleaned: " + isClean);
            Log.i("TrackerTest", "Events in the store: " + emitter.getEventStore().getSize());
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
        emitter.waitForEventStore();

        Subject subject = new Subject
                .SubjectBuilder()
                .context(getContext())
                .build();

        return new Tracker.TrackerBuilder(emitter, "myNamespace", "myAppId", getContext())
            .subject(subject)
            .platform(DevicePlatforms.InternetOfThings)
            .base64(false)
            .level(LogLevel.VERBOSE)
            .threadCount(1)
            .sessionContext(false)
            .mobileContext(false)
            .geoLocationContext(false)
            .foregroundTimeout(5)
            .backgroundTimeout(5)
            .sessionCheckInterval(15)
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
        assertEquals(DevicePlatforms.InternetOfThings, tracker.getPlatform());
        assertEquals(false, tracker.getBase64Encoded());
        assertNotNull(tracker.getEmitter());
        assertNotNull(tracker.getSubject());
        assertEquals(LogLevel.VERBOSE, tracker.getLogLevel());
        assertEquals(2, tracker.getThreadCount());
        assertEquals(false, tracker.getApplicationCrash());
        assertEquals(true, tracker.getLifecycleEvents());
        assertEquals(true, tracker.getInstallTracking());
        assertEquals(true, tracker.getApplicationContext());
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
        assertEquals(DevicePlatforms.InternetOfThings, tracker.getPlatform());

        tracker.setPlatform(DevicePlatforms.Mobile);
        assertEquals(DevicePlatforms.Mobile, tracker.getPlatform());
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
        assertFalse(id1.equals(id2));
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
            emitter.waitForEventStore();
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
        tracker.track(ScreenView.builder().build());
        Log.i("testTrackWithNoContext", "allEvents: \n" + tracker.getEmitter().getEventStore().getAllEvents());
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
        emitter.waitForEventStore();

        Tracker tracker = new Tracker.TrackerBuilder(emitter, "myNamespace", "myAppId", getContext())
            .base64(false)
            .level(LogLevel.VERBOSE)
            .sessionContext(false)
            .mobileContext(false)
            .geoLocationContext(false)
            .build();

        tracker.pauseEventTracking();
        tracker.track(ScreenView.builder().build());
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
        emitter.waitForEventStore();

        Tracker tracker = new Tracker.TrackerBuilder(emitter, "myNamespace", "myAppId", getContext())
            .base64(false)
            .level(LogLevel.VERBOSE)
            .sessionContext(true)
            .mobileContext(false)
            .geoLocationContext(false)
            .foregroundTimeout(5)
            .backgroundTimeout(5)
            .sessionCheckInterval(1)
            .timeUnit(TimeUnit.SECONDS)
            .build();

        assertNotNull(tracker.getSession());
        tracker.resumeSessionChecking();
        Thread.sleep(2000);
        tracker.pauseSessionChecking();

        mockWebServer.shutdown();
    }

    public void testTrackScreenView() throws Exception {
        Emitter emitter = new Emitter.EmitterBuilder("fake-uri", getContext())
                .option(BufferOption.Single)
                .build();
        emitter.waitForEventStore();

        Tracker tracker = new Tracker.TrackerBuilder(emitter, "myNamespace", "myAppId", getContext())
                .base64(false)
                .level(LogLevel.VERBOSE)
                .sessionContext(false)
                .mobileContext(false)
                .geoLocationContext(false)
                .screenContext(true)
                .foregroundTimeout(5)
                .backgroundTimeout(5)
                .sessionCheckInterval(1)
                .timeUnit(TimeUnit.SECONDS)
                .build();

        ScreenState screenState = tracker.getScreenState();
        assertNotNull(screenState);

        Map<String, Object> screenStateMapWrapper = screenState.getCurrentScreen(true).getMap();
        Map<String, Object> screenStateMap = (Map<String, Object>)screenStateMapWrapper.get(Parameters.DATA);
        assertEquals("Unknown", screenStateMap.get(Parameters.SCREEN_NAME));

        // Send screenView
        ScreenView screenView = ScreenView.builder().name("screen1").build();
        String screenId = (String)screenView.getData().getMap().get("id");
        tracker.track(screenView);

        screenStateMapWrapper = screenState.getCurrentScreen(true).getMap();
        screenStateMap = (Map<String, Object>)screenStateMapWrapper.get(Parameters.DATA);
        assertEquals("screen1", screenStateMap.get(Parameters.SCREEN_NAME));
        assertEquals(screenId, screenStateMap.get(Parameters.SCREEN_ID));

        // Send another screenView
        screenView = ScreenView.builder().name("screen2").build();
        String screenId1 = (String)screenView.getData().getMap().get("id");
        tracker.track(screenView);

        Map<String, Object> payload = (Map<String, Object>)screenView.getPayload().getMap().get("data");
        assertEquals("screen2", payload.get(Parameters.SCREEN_NAME));
        assertEquals(screenId1, payload.get(Parameters.SCREEN_ID));
        assertEquals("screen1", payload.get(Parameters.SV_PREVIOUS_NAME));
        assertEquals(screenId, payload.get(Parameters.SV_PREVIOUS_ID));
    }

    public void testTrackUncaughtException() throws InterruptedException {
        Thread.setDefaultUncaughtExceptionHandler(
                new TestExceptionHandler("Illegal State Exception has been thrown!")
        );

        assertEquals(
                TestExceptionHandler.class,
                Thread.getDefaultUncaughtExceptionHandler().getClass()
        );

        Emitter emitter = new Emitter.EmitterBuilder("com.acme", getContext()).build();
        emitter.waitForEventStore();

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
        emitter.waitForEventStore();

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

        public void uncaughtException(Thread t, Throwable e) {
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

    @SuppressLint("DefaultLocale")
    public String getMockServerURI(MockWebServer mockServer) {
        if (mockServer != null) {
            return String.format("%s:%d", mockServer.getHostName(), mockServer.getPort());
        }
        return null;
    }
}
