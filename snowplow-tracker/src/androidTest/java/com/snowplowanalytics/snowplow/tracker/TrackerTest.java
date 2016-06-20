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

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.utils.LogLevel;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TrackerTest extends AndroidTestCase {

    // Helper Methods

    private Tracker getTracker() {

        // Make an emitter
        Emitter emitter = new Emitter
                .EmitterBuilder("testUrl", getContext())
                .tick(0)
                .emptyLimit(0)
                .build();

        Subject subject = new Subject
                .SubjectBuilder()
                .context(getContext())
                .build();

        // Make and return the Tracker object
        return new Tracker
                .TrackerBuilder(emitter, "myNamespace", "myAppId", getContext())
                .subject(subject)
                .platform(DevicePlatforms.InternetOfThings)
                .base64(false)
                .level(LogLevel.DEBUG)
                .threadCount(20)
                .sessionCheckInterval(15)
                .backgroundTimeout(4000)
                .foregroundTimeout(20000)
                .timeUnit(TimeUnit.MILLISECONDS)
                .sessionContext(true)
                .build();
    }

    // Tests

    public void testNamespaceSet() {
        Tracker tracker = getTracker();
        assertEquals("myNamespace", tracker.getNamespace());
    }

    public void testAppIdSet() {
        Tracker tracker = getTracker();
        assertEquals("myAppId", tracker.getAppId());
    }

    public void testDevicePlatformSet() {
        Tracker tracker = getTracker();
        assertEquals(DevicePlatforms.InternetOfThings, tracker.getPlatform());
    }

    public void testBase64Set() {
        Tracker tracker = getTracker();
        assertEquals(false, tracker.getBase64Encoded());
    }

    public void testEmitterSet() {
        Tracker tracker = getTracker();
        assertNotNull(tracker.getEmitter());
    }

    public void testSubjectSet() {
        Tracker tracker = getTracker();
        assertNotNull(tracker.getSubject());
    }

    public void testVersionSet() {
        Tracker tracker = getTracker();
        assertEquals("andr-0.5.4", tracker.getTrackerVersion());
    }

    public void testLogLevelSet() {
        Tracker tracker = getTracker();
        assertEquals(LogLevel.DEBUG, tracker.getLogLevel());
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
        assertTrue(!tracker.getDataCollection());

        tracker.resumeEventTracking();
        assertTrue(tracker.getDataCollection());
    }

    public void testThreadCountSet() {
        Tracker tracker = getTracker();
        assertEquals(20, tracker.getThreadCount());
    }

    public void testTrackerSessionSet() {
        Tracker tracker = getTracker();
        Session session = tracker.getSession();

        assertNotNull(session);
        assertNotNull(session.getCurrentSessionId());
        assertNotNull(session.getSessionIndex());
        assertNotNull(session.getUserId());
        assertEquals("SQLITE", session.getSessionStorage());
        assertEquals(4000, session.getBackgroundTimeout());
        assertEquals(20000, session.getForegroundTimeout());

        Map<String, Object> sessionInfo = session.getSessionContext().getMap();
        assertTrue(sessionInfo.containsKey("schema"));
        assertTrue(sessionInfo.containsKey("data"));

        Map sessionData = session.getSessionValues();
        assertTrue(sessionData.containsKey(Parameters.SESSION_USER_ID));
        assertTrue(sessionData.containsKey(Parameters.SESSION_INDEX));
        assertTrue(sessionData.containsKey(Parameters.SESSION_ID));
        assertTrue(sessionData.containsKey(Parameters.SESSION_PREVIOUS_ID));
        assertTrue(sessionData.containsKey(Parameters.SESSION_STORAGE));
    }
}
