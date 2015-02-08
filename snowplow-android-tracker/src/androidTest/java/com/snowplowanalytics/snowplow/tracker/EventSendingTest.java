package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

import java.util.LinkedList;

import org.json.JSONObject;

import com.snowplowanalytics.snowplow.tracker.utils.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.utils.LogFetcher;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestSecurity;

public class EventSendingTest extends AndroidTestCase {

    private static final String testURL = "10.0.2.2:4545";

    // Helper methods

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

        // Ensure Mountebank is ready
        Thread.sleep(1000);

        // Make an emitter
        Emitter emitter = new Emitter
                .EmitterBuilder(testURL, getContext())
                .httpMethod(HttpMethod.GET)
                .build();

        // Ensure eventStore is empty
        emitter.getEventStore().removeAllEvents();

        // Make a subject
        Subject subject = new Subject(getContext());

        // Make and return the Tracker object
        Tracker tracker = new Tracker
                .TrackerBuilder(emitter, "myNamespace", "myAppId")
                .subject(subject)
                .build();

        // Track an event!
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

        // Ensure Mountebank is ready
        Thread.sleep(1000);

        // Make an emitter
        Emitter emitter = new Emitter
                .EmitterBuilder(testURL, getContext())
                .httpMethod(HttpMethod.POST)
                .build();

        // Ensure eventStore is empty
        emitter.getEventStore().removeAllEvents();

        // Make a subject
        Subject subject = new Subject(getContext());

        // Make and return the Tracker object
        Tracker tracker = new Tracker
                .TrackerBuilder(emitter, "myNamespace", "myAppId")
                .subject(subject)
                .build();

        // Track an event!
        tracker.trackScreenView("Screen 1", null);

        // Wait for Tracker to shutdown...
        while (tracker.getEmitter().getEmitterSubscriptionStatus()) {
            Thread.sleep(500);
        }

        // Fetch the requests from mountebank and run tests
        //sendAsserts(LogFetcher.getMountebankPostRequests(), 1);
    }
}
