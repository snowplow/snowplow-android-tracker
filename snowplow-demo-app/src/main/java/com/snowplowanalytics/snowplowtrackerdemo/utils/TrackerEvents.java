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

package com.snowplowanalytics.snowplowtrackerdemo.utils;

import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.events.EcommerceTransaction;
import com.snowplowanalytics.snowplow.tracker.events.EcommerceTransactionItem;
import com.snowplowanalytics.snowplow.tracker.events.PageView;
import com.snowplowanalytics.snowplow.tracker.events.ScreenView;
import com.snowplowanalytics.snowplow.tracker.events.Structured;
import com.snowplowanalytics.snowplow.tracker.events.TimingWithCategory;
import com.snowplowanalytics.snowplow.tracker.events.Unstructured;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Utility Class used to send all
 * combinations of Tracker Events.
 */
public class TrackerEvents {

    public static void trackAll(Tracker tracker) {
        trackPageView(tracker);
        trackStructuredEvent(tracker);
        trackScreenView(tracker);
        trackTimings(tracker);
        trackUnstructuredEvent(tracker);
        trackEcommerceEvent(tracker);
    }

    public static void trackPageView(com.snowplowanalytics.snowplow.tracker.Tracker tracker) {
        tracker.track(PageView.builder().pageUrl("pageUrl").pageTitle("pageTitle").referrer("pageReferrer").build());
        tracker.track(PageView.builder().pageUrl("pageUrl").pageTitle("pageTitle").referrer("pageReferrer").customContext(getCustomContext()).build());
        tracker.track(PageView.builder().pageUrl("pageUrl").pageTitle("pageTitle").referrer("pageReferrer").timestamp((long) 1433791172).build());
        tracker.track(PageView.builder().pageUrl("pageUrl").pageTitle("pageTitle").referrer("pageReferrer").timestamp((long) 1433791172).customContext(getCustomContext()).build());
    }

    public static void trackStructuredEvent(com.snowplowanalytics.snowplow.tracker.Tracker tracker) {
        tracker.track(Structured.builder().category("category").action("action").label("label").property("property").value(0.00).build());
        tracker.track(Structured.builder().category("category").action("action").label("label").property("property").value(0.00).customContext(getCustomContext()).build());
        tracker.track(Structured.builder().category("category").action("action").label("label").property("property").value(0.00).timestamp((long) 1433791172).build());
        tracker.track(Structured.builder().category("category").action("action").label("label").property("property").value(0.00).timestamp((long) 1433791172).customContext(getCustomContext()).build());
    }

    public static void trackScreenView(com.snowplowanalytics.snowplow.tracker.Tracker tracker) {
        tracker.track(ScreenView.builder().name("screenName").id("screenId").build());
        tracker.track(ScreenView.builder().name("screenName").id("screenId").customContext(getCustomContext()).build());
        tracker.track(ScreenView.builder().name("screenName").id("screenId").timestamp((long) 1433791172).build());
        tracker.track(ScreenView.builder().name("screenName").id("screenId").timestamp((long) 1433791172).customContext(getCustomContext()).build());
    }

    public static void trackTimings(com.snowplowanalytics.snowplow.tracker.Tracker tracker) {
        tracker.track(TimingWithCategory.builder().category("category").variable("variable").timing(1).label("label").build());
        tracker.track(TimingWithCategory.builder().category("category").variable("variable").timing(1).label("label").customContext(getCustomContext()).build());
        tracker.track(TimingWithCategory.builder().category("category").variable("variable").timing(1).label("label").timestamp((long) 1433791172).build());
        tracker.track(TimingWithCategory.builder().category("category").variable("variable").timing(1).label("label").timestamp((long) 1433791172).customContext(getCustomContext()).build());
    }

    public static void trackUnstructuredEvent(com.snowplowanalytics.snowplow.tracker.Tracker tracker) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("test-key-1", "test-value-1");
        SelfDescribingJson test = new SelfDescribingJson("iglu:com.snowplowanalytics.snowplow/test_sdj/jsonschema/1-0-1", attributes);
        tracker.track(Unstructured.builder().eventData(test).build());
        tracker.track(Unstructured.builder().eventData(test).customContext(getCustomContext()).build());
        tracker.track(Unstructured.builder().eventData(test).timestamp((long) 1433791172).build());
        tracker.track(Unstructured.builder().eventData(test).timestamp((long) 1433791172).customContext(getCustomContext()).build());
    }

    public static void trackEcommerceEvent(com.snowplowanalytics.snowplow.tracker.Tracker tracker) {
        EcommerceTransactionItem item = EcommerceTransactionItem.builder().itemId("item-1").sku("sku-1").price(35.00).quantity(1).name("Acme 1").category("Stuff").currency("AUD").build();
        List<EcommerceTransactionItem> items = new LinkedList<>();
        items.add(item);
        tracker.track(EcommerceTransaction.builder().orderId("order-1").totalValue(42.50).affiliation("affiliation").taxValue(2.50).shipping(5.00).city("Sydney").state("NSW").country("Australia").currency("AUD").items(items).build());
        tracker.track(EcommerceTransaction.builder().orderId("order-1").totalValue(42.50).affiliation("affiliation").taxValue(2.50).shipping(5.00).city("Sydney").state("NSW").country("Australia").currency("AUD").items(items).customContext(getCustomContext()).build());
        tracker.track(EcommerceTransaction.builder().orderId("order-1").totalValue(42.50).affiliation("affiliation").taxValue(2.50).shipping(5.00).city("Sydney").state("NSW").country("Australia").currency("AUD").items(item).timestamp((long) 1433791172).build());
        tracker.track(EcommerceTransaction.builder().orderId("order-1").totalValue(42.50).affiliation("affiliation").taxValue(2.50).shipping(5.00).city("Sydney").state("NSW").country("Australia").currency("AUD").items(item).timestamp((long) 1433791172).customContext(getCustomContext()).build());    }

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
