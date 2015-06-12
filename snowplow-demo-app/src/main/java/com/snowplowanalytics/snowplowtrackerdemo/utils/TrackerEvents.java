package com.snowplowanalytics.snowplowtrackerdemo.utils;

import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.events.TransactionItem;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SelfDescribingJson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TrackerEvents {

    public static void trackAll(Tracker tracker) {
        trackPageView(tracker);
        trackStructuredEvent(tracker);
        trackScreenView(tracker);
        trackTimings(tracker);
        trackUnstructuredEvent(tracker);
        trackEcommerceEvent(tracker);
    }

    public static void trackPageView(Tracker tracker) {
        tracker.trackPageView("pageUrl", "pageTitle", "pageReferrer");
        tracker.trackPageView("pageUrl", "pageTitle", "pageReferrer", getCustomContext());
        tracker.trackPageView("pageUrl", "pageTitle", "pageReferrer", (long) 1433791172);
        tracker.trackPageView("pageUrl", "pageTitle", "pageReferrer", getCustomContext(), (long) 1433791172);
    }

    public static void trackStructuredEvent(Tracker tracker) {
        tracker.trackStructuredEvent("category", "action", "label", "property", 0.00);
        tracker.trackStructuredEvent("category", "action", "label", "property", 0.00, getCustomContext());
        tracker.trackStructuredEvent("category", "action", "label", "property", 0.00, (long) 1433791172);
        tracker.trackStructuredEvent("category", "action", "label", "property", 0.00, getCustomContext(), (long) 1433791172);
    }

    public static void trackScreenView(Tracker tracker) {
        tracker.trackScreenView("screenName", "screenId");
        tracker.trackScreenView("screenName", "screenId", getCustomContext());
        tracker.trackScreenView("screenName", "screenId", (long) 1433791172);
        tracker.trackScreenView("screenName", "screenId", getCustomContext(), (long) 1433791172);
    }

    public static void trackTimings(Tracker tracker) {
        tracker.trackTimingWithCategory("category", "variable", 1, "label");
        tracker.trackTimingWithCategory("category", "variable", 1, "label", getCustomContext());
        tracker.trackTimingWithCategory("category", "variable", 1, "label", (long) 1433791172);
        tracker.trackTimingWithCategory("category", "variable", 1, "label", getCustomContext(), (long) 1433791172);
    }

    public static void trackUnstructuredEvent(Tracker tracker) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("test-key-1", "test-value-1");
        SelfDescribingJson test = new SelfDescribingJson("iglu:com.snowplowanalytics.snowplow/test_sdj/jsonschema/1-0-1", attributes);
        tracker.trackUnstructuredEvent(test);
        tracker.trackUnstructuredEvent(test, getCustomContext());
        tracker.trackUnstructuredEvent(test, (long) 1433791172);
        tracker.trackUnstructuredEvent(test, getCustomContext(), (long) 1433791172);
    }

    public static void trackEcommerceEvent(Tracker tracker) {
        TransactionItem item = new TransactionItem("item-1", "sku-1", 35.00, 1, "Acme 1", "Stuff", "AUD");
        List<TransactionItem> items = new LinkedList<>();
        items.add(item);
        tracker.trackEcommerceTransaction("order-1", 42.50, "affiliation", 2.50, 5.00, "Sydney", "NSW", "Australia", "AUD", items);
        tracker.trackEcommerceTransaction("order-1", 42.50, "affiliation", 2.50, 5.00, "Sydney", "NSW", "Australia", "AUD", items, getCustomContext());
        tracker.trackEcommerceTransaction("order-1", 42.50, "affiliation", 2.50, 5.00, "Sydney", "NSW", "Australia", "AUD", items, (long) 1433791172);
        tracker.trackEcommerceTransaction("order-1", 42.50, "affiliation", 2.50, 5.00, "Sydney", "NSW", "Australia", "AUD", items, getCustomContext(), (long) 1433791172);
    }

    /**
     * Returns a custom context.
     */
    private static List<SelfDescribingJson> getCustomContext() {
        List<SelfDescribingJson> contexts = new ArrayList<>();
        Map<String, String> attributes = new HashMap<>();
        attributes.put("snowplow", "demo-tracker");
        SelfDescribingJson json1 = new SelfDescribingJson(
                "iglu:com.snowplowanalytics.snowplow/demo_android/jsonschema/1-0-0", attributes);
        contexts.add(json1);
        return contexts;
    }
}
