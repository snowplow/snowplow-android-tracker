package com.snowplowanalytics.snowplow.tracker.android.emitter_utils;

import com.snowplowanalytics.snowplow.tracker.android.Emitter;
import com.snowplowanalytics.snowplow.tracker.android.Subject;
import com.snowplowanalytics.snowplow.tracker.android.Tracker;

import android.test.AndroidTestCase;

public class EmitterTest extends AndroidTestCase {

    private static String testURL = "87f093a.ngrok.com";

    public void testSetRequestMethod() throws Exception {
        Emitter emitter = new Emitter
                .EmitterBuilder(testURL, getContext())
                .build();
        emitter.setRequestMethod(RequestMethod.Synchronous);
    }

    public void testSendGetData() throws Exception {
        Emitter emitter = new Emitter
                .EmitterBuilder(testURL, getContext())
                .httpMethod(HttpMethod.GET)
                .build();
        Subject subject = new Subject(getContext());
        emitter.setBufferOption(BufferOption.Instant);
        Tracker tracker = new Tracker
                .TrackerBuilder(emitter, "myNamespace", "myAppId")
                .base64(true)
                .subject(subject)
                .build();
        tracker.trackScreenView("Screen 1", null);
    }

    public void testSendPostData() throws Exception {
        Emitter emitter = new Emitter
                .EmitterBuilder(testURL, getContext())
                .httpMethod(HttpMethod.POST)
                .build();
        Subject subject = new Subject(getContext());
        emitter.setBufferOption(BufferOption.Instant);
        Tracker tracker = new Tracker
                .TrackerBuilder(emitter, "myNamespace", "myAppId")
                .base64(false)
                .subject(subject)
                .build();
        tracker.trackScreenView("Screen 1", null);
    }
}