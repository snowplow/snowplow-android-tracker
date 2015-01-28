package com.snowplowanalytics.snowplow.tracker;

import com.snowplowanalytics.snowplow.tracker.emitter_utils.*;

import android.test.AndroidTestCase;

public class EmitterTest extends AndroidTestCase {

    private static String testURL = "10.0.2.2:4545";
    //private static String testURL = "87f093a.ngrok.com";

    public void testSendGetData() throws Exception {
        Emitter emitter = new Emitter
                .EmitterBuilder(testURL, getContext())
                .httpMethod(HttpMethod.GET)
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

        // Sleep added to allow for background processes to complete
        Thread.sleep(2000);
    }
}
