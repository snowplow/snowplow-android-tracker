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

import com.snowplowanalytics.snowplow.tracker.utils.LogLevel;

public class TrackerTest extends AndroidTestCase {

    // Helper Methods

    private Tracker getTracker() {

        // Make an emitter
        Emitter emitter = new com.snowplowanalytics.snowplow.tracker.Emitter
                .EmitterBuilder("testUrl", getContext(), TestEmitter.class)
                .tick(0)
                .emptyLimit(0)
                .build();

        Subject subject = new Subject
                .SubjectBuilder()
                .context(getContext())
                .build();

        // Make and return the Tracker object
        return new Tracker
                .TrackerBuilder(emitter, "myNamespace", "myAppId", getContext(), TestTracker.class)
                .subject(subject)
                .platform(DevicePlatforms.InternetOfThings)
                .base64(false)
                .level(LogLevel.DEBUG)
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
        assertEquals("andr-0.5.0", tracker.getTrackerVersion());
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
}
