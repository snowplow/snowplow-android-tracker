/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
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

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.Emitter;
import com.snowplowanalytics.snowplow.tracker.Subject;
import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestSecurity;
import com.snowplowanalytics.snowplow.tracker.events.EcommerceTransaction;
import com.snowplowanalytics.snowplow.tracker.events.EcommerceTransactionItem;
import com.snowplowanalytics.snowplow.tracker.events.PageView;
import com.snowplowanalytics.snowplow.tracker.events.ScreenView;
import com.snowplowanalytics.snowplow.tracker.events.Structured;
import com.snowplowanalytics.snowplow.tracker.events.Timing;
import com.snowplowanalytics.snowplow.tracker.events.SelfDescribing;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.storage.EventStore;
import com.snowplowanalytics.snowplow.tracker.utils.LogLevel;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EventSendingTest extends AndroidTestCase {

    // Test Setup

    private MockWebServer getMockServer(int count) throws IOException {
        EventStore eventStore = new EventStore(getContext(), 10);
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
        MockWebServer mockServer = getMockServer(28);
        Tracker tracker = getTracker(getMockServerURI(mockServer), HttpMethod.GET);

        trackStructuredEvent(tracker);
        trackUnstructuredEvent(tracker);
        trackPageView(tracker);
        trackTimings(tracker);
        trackScreenView(tracker);
        trackEcommerceEvent(tracker);

        waitForTracker(tracker);

        checkGetRequest(getRequests(mockServer, 28));
        killMockServer(mockServer);
    }

    public void testSendPost() throws Exception {
        MockWebServer mockServer = getMockServer(28);
        Tracker tracker = getTracker(getMockServerURI(mockServer), HttpMethod.POST);

        trackStructuredEvent(tracker);
        trackUnstructuredEvent(tracker);
        trackPageView(tracker);
        trackTimings(tracker);
        trackScreenView(tracker);
        trackEcommerceEvent(tracker);

        waitForTracker(tracker);

        checkPostRequest(getRequests(mockServer, 28));
        killMockServer(mockServer);
    }

    // Helpers

    public Tracker getTracker(String uri, HttpMethod method) {
        Emitter emitter = new Emitter
                .EmitterBuilder(uri, getContext())
                .option(BufferOption.Single)
                .method(method)
                .security(RequestSecurity.HTTP)
                .tick(0)
                .emptyLimit(0)
                .build();
        emitter.getEventStore().removeAllEvents();

        Subject subject = new Subject
                .SubjectBuilder()
                .context(getContext())
                .build();

        Tracker.close();
        Tracker.init(
                new Tracker.TrackerBuilder(emitter, "myNamespace", "myAppId", getContext())
                    .subject(subject)
                    .base64(false)
                    .level(LogLevel.DEBUG)
                    .sessionContext(true)
                    .mobileContext(true)
                    .geoLocationContext(true)
                    .build()
        );
        return Tracker.instance();
    }

    @SuppressLint("DefaultLocale")
    public String getMockServerURI(MockWebServer mockServer) {
        if (mockServer != null) {
            return String.format("%s:%d", mockServer.getHostName(), mockServer.getPort());
        }
        return null;
    }

    public LinkedList<RecordedRequest> getRequests(MockWebServer mockServer, int count) throws Exception {
        LinkedList<RecordedRequest> requests = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            requests.add(mockServer.takeRequest());
        }
        return requests;
    }

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

    // Event Validation

    public void checkGetRequest(LinkedList<RecordedRequest> requests) throws Exception {
        assertEquals(28, requests.size());
        for (RecordedRequest request : requests) {

            assertEquals("/i", request.getPath().substring(0, 2));
            assertEquals("GET", request.getMethod());

            JSONObject query = new JSONObject(getQueryMap(request.getPath().substring(3)));

            assertEquals("mob", query.get("p"));
            assertEquals("myAppId", query.get("aid"));
            assertEquals("myNamespace", query.get("tna"));
            assertEquals("andr-0.5.4", query.get("tv"));
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
        assertEquals(28, requests.size());
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
                assertEquals("andr-0.5.4", json.getString("tv"));
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
        assertEquals("item-1", json.getString("ti_id"));
        assertEquals("AUD", json.getString("ti_cu"));
        assertEquals("1", json.getString("ti_qu"));
        assertEquals("35.0", json.getString("ti_pr"));
        assertEquals("Stuff", json.getString("ti_ca"));
        assertEquals("sku-1", json.getString("ti_sk"));
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
            default : break;
        }
    }

    // Event Tracker Functions

    public void trackPageView(Tracker tracker) throws Exception {
        tracker.track(PageView.builder().pageUrl("pageUrl").pageTitle("pageTitle").referrer("pageReferrer").build());
        tracker.track(PageView.builder().pageUrl("pageUrl").pageTitle("pageTitle").referrer("pageReferrer").customContext(getCustomContext()).build());
        tracker.track(PageView.builder().pageUrl("pageUrl").pageTitle("pageTitle").referrer("pageReferrer").deviceCreatedTimestamp((long) 1433791172).build());
        tracker.track(PageView.builder().pageUrl("pageUrl").pageTitle("pageTitle").referrer("pageReferrer").deviceCreatedTimestamp((long) 1433791172).customContext(getCustomContext()).build());
    }

    public void trackStructuredEvent(Tracker tracker) throws Exception {
        tracker.track(Structured.builder().category("category").action("action").label("label").property("property").value(0.00).build());
        tracker.track(Structured.builder().category("category").action("action").label("label").property("property").value(0.00).customContext(getCustomContext()).build());
        tracker.track(Structured.builder().category("category").action("action").label("label").property("property").value(0.00).deviceCreatedTimestamp((long) 1433791172).build());
        tracker.track(Structured.builder().category("category").action("action").label("label").property("property").value(0.00).deviceCreatedTimestamp((long) 1433791172).customContext(getCustomContext()).build());
    }

    public void trackScreenView(Tracker tracker) throws Exception {
        tracker.track(ScreenView.builder().name("screenName").id("screenId").build());
        tracker.track(ScreenView.builder().name("screenName").id("screenId").customContext(getCustomContext()).build());
        tracker.track(ScreenView.builder().name("screenName").id("screenId").deviceCreatedTimestamp((long) 1433791172).build());
        tracker.track(ScreenView.builder().name("screenName").id("screenId").deviceCreatedTimestamp((long) 1433791172).customContext(getCustomContext()).build());
    }

    public void trackTimings(Tracker tracker) throws Exception {
        tracker.track(Timing.builder().category("category").variable("variable").timing(1).label("label").build());
        tracker.track(Timing.builder().category("category").variable("variable").timing(1).label("label").customContext(getCustomContext()).build());
        tracker.track(Timing.builder().category("category").variable("variable").timing(1).label("label").deviceCreatedTimestamp((long) 1433791172).build());
        tracker.track(Timing.builder().category("category").variable("variable").timing(1).label("label").deviceCreatedTimestamp((long) 1433791172).customContext(getCustomContext()).build());
    }

    public void trackUnstructuredEvent(Tracker tracker) throws Exception {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("test-key-1", "test-value-1");
        SelfDescribingJson test = new SelfDescribingJson("iglu:com.snowplowanalytics.snowplow/test_sdj/jsonschema/1-0-1", attributes);
        tracker.track(SelfDescribing.builder().eventData(test).build());
        tracker.track(SelfDescribing.builder().eventData(test).customContext(getCustomContext()).build());
        tracker.track(SelfDescribing.builder().eventData(test).deviceCreatedTimestamp((long) 1433791172).build());
        tracker.track(SelfDescribing.builder().eventData(test).deviceCreatedTimestamp((long) 1433791172).customContext(getCustomContext()).build());
    }

    public void trackEcommerceEvent(Tracker tracker) throws Exception {
        EcommerceTransactionItem item = EcommerceTransactionItem.builder().itemId("item-1").sku("sku-1").price(35.00).quantity(1).name("Acme 1").category("Stuff").currency("AUD").build();
        List<EcommerceTransactionItem> items = new LinkedList<>();
        items.add(item);
        tracker.track(EcommerceTransaction.builder().orderId("order-1").totalValue(42.50).affiliation("affiliation").taxValue(2.50).shipping(5.00).city("Sydney").state("NSW").country("Australia").currency("AUD").items(items).build());
        tracker.track(EcommerceTransaction.builder().orderId("order-1").totalValue(42.50).affiliation("affiliation").taxValue(2.50).shipping(5.00).city("Sydney").state("NSW").country("Australia").currency("AUD").items(items).customContext(getCustomContext()).build());
        tracker.track(EcommerceTransaction.builder().orderId("order-1").totalValue(42.50).affiliation("affiliation").taxValue(2.50).shipping(5.00).city("Sydney").state("NSW").country("Australia").currency("AUD").items(items).deviceCreatedTimestamp((long) 1433791172).build());
        tracker.track(EcommerceTransaction.builder().orderId("order-1").totalValue(42.50).affiliation("affiliation").taxValue(2.50).shipping(5.00).city("Sydney").state("NSW").country("Australia").currency("AUD").items(items).deviceCreatedTimestamp((long) 1433791172).customContext(getCustomContext()).build());
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
