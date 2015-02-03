package com.snowplowanalytics.snowplow.tracker;

import com.snowplowanalytics.snowplow.tracker.utils.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.HttpMethod;

import android.test.AndroidTestCase;

public class EmitterTest extends AndroidTestCase {

    //private static String testURL = "10.0.2.2:4545";
    private static String testURL = "77278b85.ngrok.com";

    public void testSendPostData() throws Exception {
        Emitter emitter = new Emitter
                .EmitterBuilder(testURL)
                .httpMethod(HttpMethod.POST)
                .build();
        Subject subject = new Subject(getContext());
        emitter.setBufferOption(BufferOption.Instant);

        new Tracker
                .TrackerBuilder(emitter, "myNamespace", "myAppId", getContext())
                .base64(false)
                .subject(subject)
                .build();
        Tracker.getInstance().trackScreenView("Screen 1", null);

        Thread.sleep(10000);
    }

    public void testSendGetData() throws Exception {
        Emitter emitter = new Emitter
                .EmitterBuilder(testURL)
                .httpMethod(HttpMethod.GET)
                .build();
        Subject subject = new Subject(getContext());
        emitter.setBufferOption(BufferOption.Instant);

        new Tracker
                .TrackerBuilder(emitter, "myNamespace", "myAppId", getContext())
                .base64(false)
                .subject(subject)
                .build();
        Tracker.getInstance().trackScreenView("Screen 1", null);

        Thread.sleep(10000);
    }
}
