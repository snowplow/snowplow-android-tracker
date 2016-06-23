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
import com.snowplowanalytics.snowplow.tracker.events.SelfDescribing;
import com.snowplowanalytics.snowplow.tracker.events.Structured;
import com.snowplowanalytics.snowplow.tracker.events.Timing;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;

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

    private static void trackPageView(com.snowplowanalytics.snowplow.tracker.Tracker tracker) {
        tracker.track(PageView.builder().pageUrl("pageUrl").pageTitle("pageTitle").referrer("pageReferrer").build());
        tracker.track(PageView.builder().pageUrl("pageUrl").pageTitle("pageTitle").referrer("pageReferrer").timestamp((long) 1433791172).build());
    }

    private static void trackStructuredEvent(com.snowplowanalytics.snowplow.tracker.Tracker tracker) {
        tracker.track(Structured.builder().category("category").action("action").label("label").property("property").value(0.00).build());
        tracker.track(Structured.builder().category("category").action("action").label("label").property("property").value(0.00).timestamp((long) 1433791172).build());
    }

    private static void trackScreenView(com.snowplowanalytics.snowplow.tracker.Tracker tracker) {
        tracker.track(ScreenView.builder().name("screenName").id("screenId").build());
        tracker.track(ScreenView.builder().name("screenName").id("screenId").timestamp((long) 1433791172).build());
    }

    private static void trackTimings(com.snowplowanalytics.snowplow.tracker.Tracker tracker) {
        tracker.track(Timing.builder().category("category").variable("variable").timing(1).label("label").build());
        tracker.track(Timing.builder().category("category").variable("variable").timing(1).label("label").timestamp((long) 1433791172).build());
    }

    private static void trackUnstructuredEvent(com.snowplowanalytics.snowplow.tracker.Tracker tracker) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("targetUrl", "http://a-target-url.com");
        SelfDescribingJson test = new SelfDescribingJson("iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1", attributes);
        tracker.track(SelfDescribing.builder().eventData(test).build());
        tracker.track(SelfDescribing.builder().eventData(test).timestamp((long) 1433791172).build());
    }

    private static void trackEcommerceEvent(com.snowplowanalytics.snowplow.tracker.Tracker tracker) {
        EcommerceTransactionItem item = EcommerceTransactionItem.builder().itemId("item-1").sku("sku-1").price(35.00).quantity(1).name("Acme 1").category("Stuff").currency("AUD").build();
        List<EcommerceTransactionItem> items = new LinkedList<>();
        items.add(item);

        tracker.track(EcommerceTransaction.builder().orderId("order-1").totalValue(42.50).affiliation("affiliation").taxValue(2.50).shipping(5.00).city("Sydney").state("NSW").country("Australia").currency("AUD").items(items).build());
        tracker.track(EcommerceTransaction.builder().orderId("order-1").totalValue(42.50).affiliation("affiliation").taxValue(2.50).shipping(5.00).city("Sydney").state("NSW").country("Australia").currency("AUD").items(item).timestamp((long) 1433791172).build());
    }
}
