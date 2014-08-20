package com.snowplowanalytics.snowplow.tracker.android.emitter;

import com.snowplowanalytics.snowplow.tracker.android.Subject;
import com.snowplowanalytics.snowplow.tracker.android.Tracker;
import com.snowplowanalytics.snowplow.tracker.core.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.core.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.core.emitter.RequestMethod;

import android.test.AndroidTestCase;

public class EmitterTest extends AndroidTestCase {

//    private static String testURL = "segfault.ngrok.com";
    private static String testURL = "d3rkrsqld9gmqf.cloudfront.net";

    public void testSendGetData() throws Exception {
        Emitter emitter = new Emitter(testURL);
        Subject subject = new Subject(getContext());
        emitter.setBufferOption(BufferOption.Instant);
        Tracker tracker = new Tracker(emitter, subject, "myNamespace", "myAppId", false);
        tracker.trackScreenView("Screen 1", null);
    }

    public void testSetRequestMethod() throws Exception {
        Emitter emitter = new Emitter(testURL);
        emitter.setRequestMethod(RequestMethod.Synchronous);
    }

    public void testSendPostData() throws Exception {
        Emitter emitter = new Emitter(testURL, HttpMethod.POST);
        Subject subject = new Subject(getContext());
        emitter.setBufferOption(BufferOption.Instant);
        Tracker tracker = new Tracker(emitter, subject, "myNamespace", "myAppId", false);
        tracker.trackScreenView("Screen 1", null);
    }
}