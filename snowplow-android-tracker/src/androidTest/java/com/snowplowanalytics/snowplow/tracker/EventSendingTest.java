package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.json.JSONObject;

import com.snowplowanalytics.snowplow.tracker.utils.LogFetcher;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SelfDescribingJson;

public class EventSendingTest extends AndroidTestCase {

    private static final String testURL = "10.0.2.2:4545";

    // Helper methods

    private void setup() {
        LogFetcher.deleteImposter();
        LogFetcher.createImposter();
    }

    private void checkLogs(LinkedList<JSONObject> requests, int eventCount) throws Exception {
        // Checks that the correct amount of events was received
        assertEquals(eventCount, requests.size());

        for (JSONObject request : requests) {
            int code = request.getJSONObject("response").getInt("statusCode");

            // Double checks that the response code was 200
            assertEquals(200, code);
        }
    }

    private void checkGetContents(LinkedList<JSONObject> requests) throws Exception {
        for (JSONObject request : requests) {
            JSONObject query = request.getJSONObject("request").getJSONObject("query");
            assertEquals("ue", query.getString("e"));
            assertEquals("mob", query.getString("p"));
            assertEquals("myAppId", query.getString("aid"));
            assertEquals("myNamespace", query.getString("tna"));
            assertEquals("andr-0.3.1", query.getString("tv"));
            assertEquals("English", query.getString("lang"));
        }
    }

    private List<SelfDescribingJson> getCustomContexts() {
        List<SelfDescribingJson> contexts = new ArrayList<>();

        Map<String, String> attributes = new HashMap<>();
        attributes.put("key-1", "value-1");

        SelfDescribingJson json1 = new SelfDescribingJson(
                "iglu:com.snowplowanalytics.snowplow/example/jsonschema/1-0-1", attributes);

        SelfDescribingJson json2 = new SelfDescribingJson(
                "iglu:com.snowplowanalytics.snowplow/example/jsonschema/1-0-2", json1);

        contexts.add(json1);
        contexts.add(json2);

        return contexts;
    }

    // Tests

    public void testSendGetData() throws Exception {
        setup();

        // Ensure Mountebank is ready
        Thread.sleep(1000);

        // Make an emitter
        Emitter emitter = new Emitter
                .EmitterBuilder(testURL, getContext())
                .method(HttpMethod.GET)
                .build();

        // Ensure eventStore is empty
        emitter.getEventStore().removeAllEvents();

        // Make a subject
        Subject subject = new Subject(getContext());

        // Make and return the Tracker object
        Tracker tracker = new Tracker
                .TrackerBuilder(emitter, "myNamespace", "myAppId")
                .subject(subject)
                .base64(false)
                .build();

        // Track an event!
        tracker.trackScreenView("Screen 1", null, getCustomContexts());

        // Wait for Tracker to shutdown...
        while (tracker.getEmitter().getEmitterSubscriptionStatus()) {
            Thread.sleep(500);
        }

        LinkedList<JSONObject> logs = LogFetcher.getMountebankGetRequests();

        // Check size and response codes of returned logs
        checkLogs(logs, 1);
        checkGetContents(logs);
    }

    public void testSendPostData() throws Exception {
        setup();

        // Ensure Mountebank is ready
        Thread.sleep(1000);

        // Make an emitter
        Emitter emitter = new Emitter
                .EmitterBuilder(testURL, getContext())
                .method(HttpMethod.POST)
                .build();

        // Ensure eventStore is empty
        emitter.getEventStore().removeAllEvents();

        // Make a subject
        Subject subject = new Subject(getContext());

        // Make and return the Tracker object
        Tracker tracker = new Tracker
                .TrackerBuilder(emitter, "myNamespace", "myAppId")
                .subject(subject)
                .base64(false)
                .build();

        // Track an event!
        tracker.trackScreenView("Screen 1", null);

        // Wait for Tracker to shutdown...
        while (tracker.getEmitter().getEmitterSubscriptionStatus()) {
            Thread.sleep(500);
        }

        LinkedList<JSONObject> logs = LogFetcher.getMountebankPostRequests();

        // Check size and response codes of returned logs
        checkLogs(logs, 1);
    }
}
