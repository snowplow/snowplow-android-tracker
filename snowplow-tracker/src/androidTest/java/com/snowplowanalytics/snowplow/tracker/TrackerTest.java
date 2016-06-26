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

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.events.ScreenView;
import com.snowplowanalytics.snowplow.tracker.storage.EventStore;
import com.snowplowanalytics.snowplow.tracker.tracker.ExceptionHandler;
import com.snowplowanalytics.snowplow.tracker.utils.LogLevel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class TrackerTest extends AndroidTestCase {

    // Helper Methods

    private Tracker getTracker() {
        Emitter emitter = new Emitter
                .EmitterBuilder("testUrl", getContext())
                .tick(0)
                .emptyLimit(0)
                .build();

        Subject subject = new Subject
                .SubjectBuilder()
                .context(getContext())
                .build();

        Tracker.close();

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
            .build();
    }

    // Tests

    public void testTrackerNotInit() {
        Tracker.close();

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
        Tracker tracker = getTracker();
        assertEquals("myNamespace", tracker.getNamespace());
        assertEquals("myAppId", tracker.getAppId());
        assertEquals(DevicePlatforms.InternetOfThings, tracker.getPlatform());
        assertEquals(false, tracker.getBase64Encoded());
        assertNotNull(tracker.getEmitter());
        assertNotNull(tracker.getSubject());
        assertEquals("andr-0.5.4", tracker.getTrackerVersion());
        assertEquals(LogLevel.VERBOSE, tracker.getLogLevel());
        assertEquals(2, tracker.getThreadCount());
        assertEquals(false, tracker.getApplicationCrash());
        assertEquals(true, tracker.getLifecycleEvents());
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

    public void testTrackWithNoContext() throws Exception {
        MockWebServer mockWebServer = getMockServer(1);

        Emitter emitter = new Emitter.EmitterBuilder(getMockServerURI(mockWebServer), getContext())
                .option(BufferOption.Single)
                .build();
        emitter.getEventStore().removeAllEvents();

        Tracker.close();
        Tracker tracker = new Tracker.TrackerBuilder(emitter, "myNamespace", "myAppId", getContext())
            .base64(false)
            .level(LogLevel.VERBOSE)
            .sessionContext(false)
            .mobileContext(false)
            .geoLocationContext(false)
            .build();

        tracker.track(ScreenView.builder().id("id").build());
        RecordedRequest req = mockWebServer.takeRequest(20, TimeUnit.SECONDS);

        assertNotNull(req);

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
            event.getString(Parameters.CONTEXT);
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
        emitter.getEventStore().removeAllEvents();

        Tracker.close();
        Tracker tracker = new Tracker.TrackerBuilder(emitter, "myNamespace", "myAppId", getContext())
            .base64(false)
            .level(LogLevel.VERBOSE)
            .sessionContext(false)
            .mobileContext(false)
            .geoLocationContext(false)
            .build();

        tracker.pauseEventTracking();
        tracker.track(ScreenView.builder().id("id").build());
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
        emitter.getEventStore().removeAllEvents();

        Tracker.close();
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

    public void testTrackUncaughtException() throws InterruptedException {
        Thread.setDefaultUncaughtExceptionHandler(
                new TestExceptionHandler("Illegal State Exception has been thrown!")
        );

        assertEquals(
                TestExceptionHandler.class,
                Thread.getDefaultUncaughtExceptionHandler().getClass()
        );

        Emitter emitter = new Emitter.EmitterBuilder("com.acme", getContext()).build();

        Tracker.close();
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
        Tracker.close();
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

        public void uncaughtException(Thread t, Throwable e) {
            assertEquals(this.expectedMessage, e.getMessage());
        }
    }

    // Mock Server

    public MockWebServer getMockServer(int count) throws IOException {
        EventStore eventStore = new EventStore(getContext(), 10);
        eventStore.removeAllEvents();

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
