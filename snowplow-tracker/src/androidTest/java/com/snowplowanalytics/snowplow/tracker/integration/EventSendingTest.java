/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker.integration;

import static org.junit.Assert.assertNotEquals;

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.TestUtils;
import com.snowplowanalytics.snowplow.tracker.BuildConfig;
import com.snowplowanalytics.snowplow.internal.emitter.Emitter;
import com.snowplowanalytics.snowplow.internal.tracker.Subject;
import com.snowplowanalytics.snowplow.internal.tracker.Tracker;
import com.snowplowanalytics.snowplow.emitter.BufferOption;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.Protocol;
import com.snowplowanalytics.snowplow.event.ConsentDocument;
import com.snowplowanalytics.snowplow.event.ConsentGranted;
import com.snowplowanalytics.snowplow.event.ConsentWithdrawn;
import com.snowplowanalytics.snowplow.event.EcommerceTransaction;
import com.snowplowanalytics.snowplow.event.EcommerceTransactionItem;
import com.snowplowanalytics.snowplow.event.PageView;
import com.snowplowanalytics.snowplow.event.ScreenView;
import com.snowplowanalytics.snowplow.event.SelfDescribing;
import com.snowplowanalytics.snowplow.event.Structured;
import com.snowplowanalytics.snowplow.event.Timing;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.internal.emitter.storage.SQLiteEventStore;
import com.snowplowanalytics.snowplow.emitter.EventStore;
import com.snowplowanalytics.snowplow.tracker.LogLevel;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EventSendingTest extends AndroidTestCase {

    private static Tracker tracker;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        try {
            if (tracker == null) return;
            Emitter emitter = tracker.getEmitter();
            tracker.close();
            boolean isClean = emitter.getEventStore().removeAllEvents();
            Log.i("TrackerTest", "Tracker closed - EventStore cleaned: " + isClean);
            Log.i("TrackerTest", "Events in the store: " + emitter.getEventStore().getSize());
        } catch(IllegalStateException e) {
            Log.i("TrackerTest", "Tracker already closed.");
        }
    }

    // Test Setup

    private MockWebServer getMockServer(int count) throws IOException {
        EventStore eventStore = new SQLiteEventStore(getContext(), "namespace");
        eventStore.removeAllEvents();

        MockWebServer mockServer = new MockWebServer();
        mockServer.start();

        MockResponse mockResponse = new MockResponse().setResponseCode(200);
        for (int i = 0; i < count; i++) {
            mockServer.enqueue(mockResponse);
        }

        return mockServer;
    }

    public void killMockServer(MockWebServer mockServer) throws IOException {
        mockServer.shutdown();
    }

    // Tests

    public void testSendGet() throws Exception {
        MockWebServer mockServer = getMockServer(14);
        Tracker tracker = getTracker("myNamespace", getMockServerURI(mockServer), HttpMethod.GET);

        trackStructuredEvent(tracker);
        trackUnstructuredEvent(tracker);
        trackPageView(tracker);
        trackTimings(tracker);
        trackScreenView(tracker);
        trackEcommerceEvent(tracker);
        trackConsentGranted(tracker);
        trackConsentWithdrawn(tracker);

        waitForTracker(tracker);

        checkGetRequest(getRequests(mockServer, 14));
        killMockServer(mockServer);
    }

    public void testSendPost() throws Exception {
        MockWebServer mockServer = getMockServer(14);
        Tracker tracker = getTracker("myNamespace", getMockServerURI(mockServer), HttpMethod.POST);

        trackStructuredEvent(tracker);
        trackUnstructuredEvent(tracker);
        trackPageView(tracker);
        trackTimings(tracker);
        trackScreenView(tracker);
        trackEcommerceEvent(tracker);
        trackConsentGranted(tracker);
        trackConsentWithdrawn(tracker);

        waitForTracker(tracker);

        checkPostRequest(getRequests(mockServer, 14));
        killMockServer(mockServer);
    }

    public void testSessionContext() throws Exception {
        MockWebServer mockServer = getMockServer(14);
        Tracker tracker = getTracker("namespaceSessionTest", getMockServerURI(mockServer), HttpMethod.POST);

        tracker.track(new ScreenView("screenName_1"));
        tracker.track(new Structured("category_1", "action_1"));
        tracker.startNewSession();
        Thread.sleep(1000);
        tracker.track(new Structured("category_2", "action_2"));
        tracker.track(new Structured("category_3", "action_3"));

        waitForTracker(tracker);

        LinkedList<RecordedRequest> requests = getRequests(mockServer, 4);
        JSONObject screenViewSessionData = null;
        JSONObject structSessionData_1 = null;
        JSONObject structSessionData_2 = null;
        JSONObject structSessionData_3 = null;

        for (RecordedRequest request : requests) {
            JSONObject data = new JSONObject(request.getBody().readUtf8()).getJSONArray("data").getJSONObject(0);
            String eventType = (String) data.get("e");
            JSONArray contexts = new JSONObject((String) data.get("co")).getJSONArray("data");

            switch (eventType) {
                case "ue" : screenViewSessionData = getSessionData(contexts);
                    break;
                case "se" :
                    String category = (String) data.get("se_ca");
                    switch (category) {
                        case "category_1":
                            structSessionData_1 = getSessionData(contexts);
                            break;
                        case "category_2":
                            structSessionData_2 = getSessionData(contexts);
                            break;
                        case "category_3":
                            structSessionData_3 = getSessionData(contexts);
                            break;
                    }
                    break;
                default : break;
            }

        }

        assertEquals(1, screenViewSessionData.get("sessionIndex"));
        assertEquals(2, structSessionData_2.get("sessionIndex"));

        assertEquals(screenViewSessionData.get("firstEventId"), structSessionData_1.get("firstEventId"));
        assertNotEquals(screenViewSessionData.get("firstEventId"), structSessionData_2.get("firstEventId"));
        assertEquals(structSessionData_2.get("firstEventId"), structSessionData_3.get("firstEventId"));

        assertEquals(1, screenViewSessionData.get("eventIndex"));
        assertEquals(2, structSessionData_1.get("eventIndex"));
        assertEquals(1, structSessionData_2.get("eventIndex"));
        assertEquals(2, structSessionData_3.get("eventIndex"));

        assertEquals(screenViewSessionData.get("firstEventTimestamp"), structSessionData_1.get("firstEventTimestamp"));
        assertNotEquals(screenViewSessionData.get("firstEventTimestamp"), structSessionData_2.get("firstEventTimestamp"));
        assertEquals(structSessionData_2.get("firstEventTimestamp"), structSessionData_3.get("firstEventTimestamp"));

        assertEquals(structSessionData_1.get("sessionId"), structSessionData_2.get("previousSessionId"));

        killMockServer(mockServer);
    }

    // Helpers

    @NonNull
    public Tracker getTracker(String namespace, String uri, HttpMethod method) {
        TestUtils.createSessionSharedPreferences(getContext(), namespace);

        Emitter emitter = new Emitter(getContext(), uri, new Emitter.EmitterBuilder()
                .option(BufferOption.Single)
                .method(method)
                .security(Protocol.HTTP)
                .tick(0)
                .emptyLimit(0)
        );

        Subject subject = new Subject(getContext(), null);

        if (tracker != null) tracker.close();
        tracker = new Tracker(new Tracker.TrackerBuilder(emitter, namespace, "myAppId", getContext())
                .subject(subject)
                .base64(false)
                .level(LogLevel.DEBUG)
                .sessionContext(true)
                .mobileContext(true)
                .geoLocationContext(false)
        );
        emitter.getEventStore().removeAllEvents();
        return tracker;
    }

    @SuppressLint("DefaultLocale")
    @Nullable
    public String getMockServerURI(MockWebServer mockServer) {
        if (mockServer != null) {
            return String.format("%s:%d", mockServer.getHostName(), mockServer.getPort());
        }
        return null;
    }

    @NonNull
    public LinkedList<RecordedRequest> getRequests(MockWebServer mockServer, int count) throws Exception {
        LinkedList<RecordedRequest> requests = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            RecordedRequest request = mockServer.takeRequest(60, TimeUnit.SECONDS);
            if (request == null) {
                fail("MockWebServer didn't receive events.");
            }
            requests.add(request);
        }
        return requests;
    }

    @NonNull
    public Map<String, String> getQueryMap(String query) throws Exception {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(URLDecoder.decode(name, "UTF-8"), URLDecoder.decode(value, "UTF-8"));
        }
        return map;
    }

    public void waitForTracker(Tracker tracker) throws Exception {
        int counter = 0;
        while (!tracker.getEmitter().getEmitterStatus()) {
            Thread.sleep(500);
            counter++;
            if (counter > 10) {
                return;
            }
        }
        counter = 0;
        while (tracker.getEmitter().getEmitterStatus()) {
            Thread.sleep(500);
            counter++;
            if (counter > 10) {
                return;
            }
        }
        Thread.sleep(500);
        tracker.pauseEventTracking();
    }

    public JSONObject getSessionData(JSONArray contexts) throws JSONException {
        for (int i = 0; i < contexts.length(); i++) {
            String sessionSchema = "iglu:com.snowplowanalytics.snowplow/client_session/jsonschema/1-0-2";
            JSONObject context = (JSONObject) contexts.get(i);
            String schema = (String) context.get("schema");

            if (schema.equals(sessionSchema)) {
                return (JSONObject) context.get("data");
            }
        }
        return null;
    }

    // Event Validation

    public void checkGetRequest(LinkedList<RecordedRequest> requests) throws Exception {
        assertEquals(14, requests.size());
        for (RecordedRequest request : requests) {

            assertEquals("/i", request.getPath().substring(0, 2));
            assertEquals("GET", request.getMethod());

            JSONObject query = new JSONObject(getQueryMap(request.getPath().substring(3)));

            assertEquals("mob", query.get("p"));
            assertEquals("myAppId", query.get("aid"));
            assertEquals("myNamespace", query.get("tna"));
            assertEquals(BuildConfig.TRACKER_LABEL, query.get("tv"));
            assertEquals("English", query.get("lang"));
            assertTrue(query.has("dtm"));
            assertTrue(query.has("stm"));
            assertTrue(query.has("e"));
            assertTrue(query.has("co"));
            assertTrue(query.has("eid"));

            String eventType = query.get("e").toString();

            switch (eventType) {
                case "pv" : checkPageView(query);
                    break;
                case "ue" : checkUnstructuredEvent(query);
                    break;
                case "se" : checkStructuredEvent(query);
                    break;
                case "tr" : checkEcommerceEvent(query);
                    break;
                case "ti" : checkEcommerceItemEvent(query);
                    break;
                default : break;
            }
        }
    }

    public void checkPostRequest(LinkedList<RecordedRequest> requests) throws Exception {
        assertEquals(14, requests.size());
        for (RecordedRequest request : requests) {
            assertEquals("/com.snowplowanalytics.snowplow/tp2", request.getPath());
            assertEquals("POST", request.getMethod());

            JSONObject body = new JSONObject(request.getBody().readUtf8());
            assertEquals("iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-4", body.getString("schema"));

            JSONArray data = body.getJSONArray("data");
            for (int i = 0; i < data.length(); i++) {
                JSONObject json = data.getJSONObject(i);
                assertEquals("mob", json.getString("p"));
                assertEquals("myAppId", json.getString("aid"));
                assertEquals("myNamespace", json.getString("tna"));
                assertEquals(BuildConfig.TRACKER_LABEL, json.getString("tv"));
                assertEquals("English", json.getString("lang"));
                assertTrue(json.has("dtm"));
                assertTrue(json.has("stm"));
                assertTrue(json.has("e"));
                assertTrue(json.has("co"));
                assertTrue(json.has("eid"));

                String eventType = json.getString("e");
                switch (eventType) {
                    case "pv" : checkPageView(json);
                        break;
                    case "ue" : checkUnstructuredEvent(json);
                        break;
                    case "se" : checkStructuredEvent(json);
                        break;
                    case "tr" : checkEcommerceEvent(json);
                        break;
                    case "ti" : checkEcommerceItemEvent(json);
                        break;
                    default : break;
                }
            }
        }
    }

    public void checkPageView(JSONObject json) throws Exception {
        assertEquals("pageUrl", json.getString("url"));
        assertEquals("pageReferrer", json.getString("refr"));
        assertEquals("pageTitle", json.getString("page"));
    }

    public void checkScreenView(JSONObject json) throws Exception {
        assertEquals("screenId", json.getString("id"));
        assertEquals("screenName", json.getString("name"));
    }

    public void checkTimings(JSONObject json) throws Exception {
        assertEquals(1, json.getInt("timing"));
        assertEquals("variable", json.getString("variable"));
        assertEquals("category", json.getString("category"));
        assertEquals("label", json.getString("label"));
    }

    public void checkStructuredEvent(JSONObject json) throws Exception {
        assertEquals("property", json.getString("se_pr"));
        assertEquals("0.0", json.getString("se_va"));
        assertEquals("label", json.getString("se_la"));
        assertEquals("category", json.getString("se_ca"));
        assertEquals("action", json.getString("se_ac"));
    }

    public void checkTestEvent(JSONObject json) throws Exception {
        assertEquals("test-value-1", json.getString("test-key-1"));
    }

    public void checkEcommerceEvent(JSONObject json) throws Exception {
        assertEquals("AUD", json.getString("tr_cu"));
        assertEquals("5.0", json.getString("tr_sh"));
        assertEquals("Australia", json.getString("tr_co"));
        assertEquals("2.5", json.getString("tr_tx"));
        assertEquals("affiliation", json.getString("tr_af"));
        assertEquals("order-1", json.getString("tr_id"));
        assertEquals("Sydney", json.getString("tr_ci"));
        assertEquals("42.5", json.getString("tr_tt"));
        assertEquals("NSW", json.getString("tr_st"));
    }

    public void checkEcommerceItemEvent(JSONObject json) throws Exception {
        assertEquals("Acme 1", json.getString("ti_nm"));
        assertEquals("order-1", json.getString("ti_id"));
        assertEquals("AUD", json.getString("ti_cu"));
        assertEquals("1", json.getString("ti_qu"));
        assertEquals("35.0", json.getString("ti_pr"));
        assertEquals("Stuff", json.getString("ti_ca"));
        assertEquals("sku-1", json.getString("ti_sk"));
    }

    public void checkConsentGrantedEvent(JSONObject json) throws Exception {
        assertEquals("gexpiry", json.getString("expiry"));
    }

    public void checkConsentWithdrawnEvent(JSONObject json) throws Exception {
        assertFalse(json.getBoolean("all"));
    }

    public void checkUnstructuredEvent(JSONObject json) throws Exception {
        System.out.println(json);

        JSONObject unstructEvent = new JSONObject(json.getString("ue_pr"));

        System.out.println(unstructEvent);

        assertEquals("iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0", unstructEvent.getString("schema"));
        String innerSchema = unstructEvent.getJSONObject("data").getString("schema");
        JSONObject innerData = unstructEvent.getJSONObject("data").getJSONObject("data");
        switch (innerSchema) {
            case "iglu:com.snowplowanalytics.snowplow/screen_view/jsonschema/1-0-0" : checkScreenView(innerData);
                break;
            case "iglu:com.snowplowanalytics.snowplow/timing/jsonschema/1-0-0" : checkTimings(innerData);
                break;
            case "iglu:com.snowplowanalytics.snowplow/test_sdj/jsonschema/1-0-1" : checkTestEvent(innerData);
                break;
            case "iglu:com.snowplowanalytics.snowplow/consent_granted/jsonschema/1-0-0" : checkConsentGrantedEvent(innerData);
                break;
            case "iglu:com.snowplowanalytics.snowplow/consent_withdrawn/jsonschema/1-0-0" : checkConsentWithdrawnEvent(innerData);
                break;
            default : break;
        }
    }

    // Event Tracker Functions

    public void trackPageView(Tracker tracker) throws Exception {
        tracker.track(new PageView("pageUrl").pageTitle("pageTitle").referrer("pageReferrer"));
        tracker.track(new PageView("pageUrl").pageTitle("pageTitle").referrer("pageReferrer").contexts(getCustomContext()));
    }

    public void trackStructuredEvent(Tracker tracker) {
        tracker.track(new Structured("category", "action").label("label").property("property").value(0.00));
        tracker.track(new Structured("category", "action").label("label").property("property").value(0.00).contexts(getCustomContext()));
    }

    public void trackScreenView(Tracker tracker) {
        tracker.track(new ScreenView("screenName"));
        tracker.track(new ScreenView("screenName").contexts(getCustomContext()));
    }

    public void trackTimings(Tracker tracker) {
        tracker.track(new Timing("category", "variable", 1).label("label"));
        tracker.track(new Timing("category", "variable", 1).label("label").contexts(getCustomContext()));
    }

    public void trackUnstructuredEvent(Tracker tracker) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("test-key-1", "test-value-1");
        SelfDescribingJson test = new SelfDescribingJson("iglu:com.snowplowanalytics.snowplow/test_sdj/jsonschema/1-0-1", attributes);
        tracker.track(new SelfDescribing(test));
        tracker.track(new SelfDescribing(test).contexts(getCustomContext()));
    }

    public void trackEcommerceEvent(Tracker tracker) {
        EcommerceTransactionItem item = new EcommerceTransactionItem("sku-1", 35.00, 1).name("Acme 1").category("Stuff").currency("AUD");
        List<EcommerceTransactionItem> items = new LinkedList<>();
        items.add(item);
        tracker.track(new EcommerceTransaction("order-1", 42.50, items).affiliation("affiliation").taxValue(2.50).shipping(5.00).city("Sydney").state("NSW").country("Australia").currency("AUD"));
        tracker.track(new EcommerceTransaction("order-1", 42.50, items).affiliation("affiliation").taxValue(2.50).shipping(5.00).city("Sydney").state("NSW").country("Australia").currency("AUD").contexts(getCustomContext()));
    }

    public void trackConsentGranted(Tracker tracker) {
        List<ConsentDocument> documents = new LinkedList<>();
        documents.add(new ConsentDocument("granted context id 1", "granted context version 1")
                .documentDescription("granted context desc 1")
                .documentName("granted context name 1"));
        documents.add(new ConsentDocument("granted context id 2", "granted context version 2")
                .documentDescription("granted context desc 2")
                .documentName("granted context name 2"));

        tracker.track(new ConsentGranted("gexpiry", "gid", "dversion").documentDescription("gdesc").documentName("dname").documents(documents));
        tracker.track(new ConsentGranted("gexpiry", "gid", "dversion").documentDescription("gdesc").documentName("dname").documents(documents).contexts(getCustomContext()));
    }

    public void trackConsentWithdrawn(Tracker tracker) {
        List<ConsentDocument> documents = new LinkedList<>();
        documents.add(new ConsentDocument("withdrawn context id 1", "withdrawn context version 1")
                .documentDescription("withdrawn context desc 1")
                .documentName("withdrawn context name 1"));
        documents.add(new ConsentDocument("withdrawn context id 2", "withdrawn context version 2")
                .documentDescription("withdrawn context desc 2")
                .documentName("withdrawn context name 2"));

        tracker.track(new ConsentWithdrawn(false, "gid", "dversion").documentDescription("gdesc").documentName("dname").documents(documents));
        tracker.track(new ConsentWithdrawn(false, "gid", "dversion").documentDescription("gdesc").documentName("dname").documents(documents).contexts(getCustomContext()));
    }

    public List<SelfDescribingJson> getCustomContext() {
        List<SelfDescribingJson> contexts = new ArrayList<>();
        Map<String, String> attributes1 = new HashMap<>();
        attributes1.put("key-1", "value-1");
        SelfDescribingJson json1 = new SelfDescribingJson(
                "iglu:com.snowplowanalytics.snowplow/example_1/jsonschema/1-0-1", attributes1);
        contexts.add(json1);
        return contexts;
    }
}
