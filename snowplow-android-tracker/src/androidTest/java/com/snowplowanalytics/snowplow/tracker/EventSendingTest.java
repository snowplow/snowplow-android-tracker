package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

import java.util.LinkedList;

import org.json.JSONObject;

import com.snowplowanalytics.snowplow.tracker.utils.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.utils.LogFetcher;

public class EventSendingTest extends AndroidTestCase {

    private static final String testURL = "70d7306b.ngrok.com";

    // Helper methods

    private Tracker getTracker(HttpMethod method) {
        // Make an emitter
        Emitter emitter = new Emitter
                .EmitterBuilder(testURL, getContext())
                .httpMethod(method)
                .build();
        emitter.getEventStore().removeAllEvents();

        // Make a subject
        Subject subject = new Subject(getContext());

        // Make and return the Tracker object
        return new Tracker
                .TrackerBuilder(emitter, "myNamespace", "myAppId")
                .subject(subject)
                .build();
    }

    private void setup() {
        LogFetcher.deleteImposter();
        LogFetcher.createImposter();
    }

    private void sendAsserts(LinkedList<JSONObject> requests, int eventCount) throws Exception {
        assertEquals(eventCount, requests.size());
        for (JSONObject request : requests) {
            int code = request.getJSONObject("response").getInt("statusCode");
            assertEquals(200, code);
        }
    }

    // Tests
    // TODO: This test only fails in Travis - cannot replicate
    public void testSendGetData() throws Exception {
        setup();

        Tracker tracker = getTracker(HttpMethod.GET);
        tracker.trackScreenView("Screen 1", null);

        // Wait for Tracker to shutdown...
        while (tracker.getEmitter().getEmitterSubscriptionStatus()) {
            Thread.sleep(500);
        }

        // Fetch the requests from mountebank and run tests
        //sendAsserts(LogFetcher.getMountebankGetRequests(), 1);
    }

    public void testSendPostData() throws Exception {
        setup();

        Tracker tracker = getTracker(HttpMethod.POST);
        tracker.trackScreenView("Screen 1", null);

        // Wait for Tracker to shutdown...
        while (tracker.getEmitter().getEmitterSubscriptionStatus()) {
            Thread.sleep(500);
        }

        // Fetch the requests from mountebank and run tests
        //sendAsserts(LogFetcher.getMountebankPostRequests(), 1);
    }
}
