package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

public class TrackerTest extends AndroidTestCase {

    // Helper Methods

    private Tracker getTracker() {
        // Make an emitter
        Emitter emitter = new com.snowplowanalytics.snowplow.tracker.Emitter.EmitterBuilder("testUrl", getContext(), TestEmitter.class)
                .build();

        Subject subject = new Subject(getContext());

        // Make and return the Tracker object
        return new Tracker
                .TrackerBuilder(emitter, "myNamespace", "myAppId")
                .subject(subject)
                .platform(DevicePlatforms.InternetOfThings)
                .base64(false)
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
        assertEquals("andr-0.3.0", tracker.getTrackerVersion());
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
