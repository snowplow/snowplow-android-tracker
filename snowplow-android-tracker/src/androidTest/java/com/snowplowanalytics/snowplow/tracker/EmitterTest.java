package com.snowplowanalytics.snowplow.tracker;

import com.snowplowanalytics.snowplow.tracker.utils.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.HttpMethod;

import android.test.AndroidTestCase;

public class EmitterTest extends AndroidTestCase {

    private static String testURL = "10.0.2.2:4545";
    //private static String testURL = "70d7306b.ngrok.com";

    public void testSendGetData() throws Exception {
        Emitter emitter = new Emitter
                .EmitterBuilder(testURL, getContext())
                .httpMethod(HttpMethod.POST)
                .build();

        Subject subject = new Subject(getContext());

        emitter.setBufferOption(BufferOption.Default);

        Tracker tracker = new Tracker
                .TrackerBuilder(emitter, "myNamespace", "myAppId")
                .base64(false)
                .subject(subject)
                .build();

        for (int i = 0; i < 10; i++)
            tracker.trackScreenView("Screen 1", null);

        while (tracker.getEmitter().getEmitterSubscriptionStatus()) {
            Thread.sleep(500);
        }
    }
}
