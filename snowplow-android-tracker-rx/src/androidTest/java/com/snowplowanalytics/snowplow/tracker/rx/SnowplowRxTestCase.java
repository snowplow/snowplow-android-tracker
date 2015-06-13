package com.snowplowanalytics.snowplow.tracker.rx;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.BufferOption;
import com.snowplowanalytics.snowplow.tracker.LogLevel;
import com.snowplowanalytics.snowplow.tracker.RequestSecurity;
import com.snowplowanalytics.snowplow.tracker.Subject;
import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.events.TransactionItem;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SelfDescribingJson;

import com.snowplowanalytics.snowplow.tracker.rx.utils.LogFetcher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SnowplowRxTestCase extends AndroidTestCase {

    private static final String testURL = "10.0.2.2:4545";

    // Tracker Builder

    public com.snowplowanalytics.snowplow.tracker.Tracker getTracker(
            com.snowplowanalytics.snowplow.tracker.Emitter emitter, Subject subject) {
        return new Tracker
            .TrackerBuilder(emitter, "myNamespace", "myAppId")
            .subject(subject)
            .base64(false)
            .level(LogLevel.DEBUG)
            .build();
    }

    public com.snowplowanalytics.snowplow.tracker.Emitter getEmitter(HttpMethod method,
                                                 BufferOption option, RequestSecurity security) {
        return new Emitter
            .EmitterBuilder(testURL, getContext())
            .option(option)
            .method(method)
            .security(security)
            .tick(1)
            .emptyLimit(2)
            .build();
    }

    public Subject getSubject() {
        return new Subject
            .SubjectBuilder()
            .context(getContext())
            .build();
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


    // Mountebank Setup/Helpers

    public void setup() throws Exception {
        LogFetcher.deleteImposter();
        LogFetcher.createImposter();
        Thread.sleep(1000);
    }

    public void checkLogs(LinkedList<JSONObject> requests, int eventCount) throws Exception {
        assertEquals(eventCount, requests.size());
        for (JSONObject request : requests) {
            int code = request.getJSONObject("response").getInt("statusCode");
            assertEquals(200, code);
        }
    }

    public void checkGetRequest(LinkedList<JSONObject> requests) throws Exception {
        for (JSONObject request : requests) {
            JSONObject request1 = request.getJSONObject("request");
            JSONObject query = request1.getJSONObject("query");

            assertEquals("/i", request1.getString("path"));
            assertEquals("GET", request1.getString("method"));

            assertEquals("mob", query.getString("p"));
            assertEquals("myAppId", query.getString("aid"));
            assertEquals("myNamespace", query.getString("tna"));
            assertEquals("andr-0.4.0", query.getString("tv"));
            assertEquals("English", query.getString("lang"));
            assertTrue(query.has("dtm"));
            assertTrue(query.has("stm"));
            assertTrue(query.has("e"));
            assertTrue(query.has("co"));
            assertTrue(query.has("eid"));

            String eventType = query.getString("e");
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

    public void checkPostRequest(LinkedList<JSONObject> requests) throws Exception {
        for (JSONObject request : requests) {
            JSONObject request1 = request.getJSONObject("request");

            assertEquals("/com.snowplowanalytics.snowplow/tp2", request1.getString("path"));
            assertEquals("POST", request1.getString("method"));

            JSONObject body = new JSONObject(request1.getString("body"));
            assertEquals("iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-3", body.getString("schema"));

            JSONArray data = body.getJSONArray("data");
            for (int i = 0; i < data.length(); i++) {
                JSONObject json = data.getJSONObject(i);
                assertEquals("mob", json.getString("p"));
                assertEquals("myAppId", json.getString("aid"));
                assertEquals("myNamespace", json.getString("tna"));
                assertEquals("andr-0.4.0", json.getString("tv"));
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
        assertEquals("1", json.getString("timing"));
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
        assertEquals("1.0", json.getString("ti_qu"));
        assertEquals("35.0", json.getString("ti_pr"));
        assertEquals("Stuff", json.getString("ti_ca"));
        assertEquals("sku-1", json.getString("ti_sk"));
    }

    public void checkUnstructuredEvent(JSONObject json) throws Exception {
        JSONObject unstructEvent = new JSONObject(json.getString("ue_pr"));
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

    public void trackPageView(com.snowplowanalytics.snowplow.tracker.Tracker tracker) throws Exception {
        tracker.trackPageView("pageUrl", "pageTitle", "pageReferrer");
        tracker.trackPageView("pageUrl", "pageTitle", "pageReferrer", getCustomContext());
        tracker.trackPageView("pageUrl", "pageTitle", "pageReferrer", (long) 1433791172);
        tracker.trackPageView("pageUrl", "pageTitle", "pageReferrer", getCustomContext(), (long) 1433791172);
    }

    public void trackStructuredEvent(com.snowplowanalytics.snowplow.tracker.Tracker tracker) throws Exception {
        tracker.trackStructuredEvent("category", "action", "label", "property", 0.00);
        tracker.trackStructuredEvent("category", "action", "label", "property", 0.00, getCustomContext());
        tracker.trackStructuredEvent("category", "action", "label", "property", 0.00, (long) 1433791172);
        tracker.trackStructuredEvent("category", "action", "label", "property", 0.00, getCustomContext(), (long) 1433791172);
    }

    public void trackScreenView(com.snowplowanalytics.snowplow.tracker.Tracker tracker) throws Exception {
        tracker.trackScreenView("screenName", "screenId");
        tracker.trackScreenView("screenName", "screenId", getCustomContext());
        tracker.trackScreenView("screenName", "screenId", (long) 1433791172);
        tracker.trackScreenView("screenName", "screenId", getCustomContext(), (long) 1433791172);
    }

    public void trackTimings(com.snowplowanalytics.snowplow.tracker.Tracker tracker) throws Exception {
        tracker.trackTimingWithCategory("category", "variable", 1, "label");
        tracker.trackTimingWithCategory("category", "variable", 1, "label", getCustomContext());
        tracker.trackTimingWithCategory("category", "variable", 1, "label", (long) 1433791172);
        tracker.trackTimingWithCategory("category", "variable", 1, "label", getCustomContext(), (long) 1433791172);
    }

    public void trackUnStructuredEvent(com.snowplowanalytics.snowplow.tracker.Tracker tracker) throws Exception {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("test-key-1", "test-value-1");
        SelfDescribingJson test = new SelfDescribingJson("iglu:com.snowplowanalytics.snowplow/test_sdj/jsonschema/1-0-1", attributes);
        tracker.trackUnstructuredEvent(test);
        tracker.trackUnstructuredEvent(test, getCustomContext());
        tracker.trackUnstructuredEvent(test, (long) 1433791172);
        tracker.trackUnstructuredEvent(test, getCustomContext(), (long) 1433791172);
    }

    public void trackEcommerceEvent(com.snowplowanalytics.snowplow.tracker.Tracker tracker) throws Exception {
        TransactionItem item = new TransactionItem("item-1", "sku-1", 35.00, 1, "Acme 1", "Stuff", "AUD");
        List<TransactionItem> items = new LinkedList<>();
        items.add(item);
        tracker.trackEcommerceTransaction("order-1", 42.50, "affiliation", 2.50, 5.00, "Sydney", "NSW", "Australia", "AUD", items);
        tracker.trackEcommerceTransaction("order-1", 42.50, "affiliation", 2.50, 5.00, "Sydney", "NSW", "Australia", "AUD", items, getCustomContext());
        tracker.trackEcommerceTransaction("order-1", 42.50, "affiliation", 2.50, 5.00, "Sydney", "NSW", "Australia", "AUD", items, (long) 1433791172);
        tracker.trackEcommerceTransaction("order-1", 42.50, "affiliation", 2.50, 5.00, "Sydney", "NSW", "Australia", "AUD", items, getCustomContext(), (long) 1433791172);
    }
}
