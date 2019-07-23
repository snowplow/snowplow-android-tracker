/*
 * Copyright (c) 2015-2019 Snowplow Analytics Ltd. All rights reserved.
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
import com.snowplowanalytics.snowplow.tracker.events.ConsentDocument;
import com.snowplowanalytics.snowplow.tracker.events.ConsentGranted;
import com.snowplowanalytics.snowplow.tracker.events.ConsentWithdrawn;
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
import java.util.UUID;

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
        trackConsentGranted(tracker);
        trackConsentWithdrawn(tracker);
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
        tracker.track(ScreenView.builder().name("screenName").id(UUID.randomUUID().toString()).build());
        tracker.track(ScreenView.builder().name("screenName").id(UUID.randomUUID().toString()).timestamp((long) 1433791172).build());
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

    private static void trackConsentGranted(com.snowplowanalytics.snowplow.tracker.Tracker tracker) {
        List<ConsentDocument> documents = new LinkedList<>();
        documents.add(ConsentDocument.builder()
                .documentDescription("granted context desc 1")
                .documentId("granted context id 1")
                .documentName("granted context name 1")
                .documentVersion("granted context version 1")
                .build());
        documents.add(ConsentDocument.builder()
                .documentDescription("granted context desc 2")
                .documentId("granted context id 2")
                .documentName("granted context name 2")
                .documentVersion("granted context version 2")
                .build());

        ConsentGranted event = ConsentGranted.builder()
                .expiry("2018-05-08T18:12:02+00:00")
                .documentDescription("granted event doc description")
                .documentId("granted event doc id")
                .documentName("granted event doc name")
                .documentVersion("granted event doc version")
                .consentDocuments(documents)
                .build();
    }

    private static void trackConsentWithdrawn(com.snowplowanalytics.snowplow.tracker.Tracker tracker) {
        List<ConsentDocument> documents = new LinkedList<>();
        documents.add(ConsentDocument.builder()
                .documentDescription("withdrawn context desc 1")
                .documentId("withdrawn context id 1")
                .documentName("withdrawn context name 1")
                .documentVersion("withdrawn context version 1")
                .build());
        documents.add(ConsentDocument.builder()
                .documentDescription("withdrawn context desc 2")
                .documentId("withdrawn context id 2")
                .documentName("withdrawn context name 2")
                .documentVersion("withdrawn context version 2")
                .build());
        ConsentWithdrawn event = ConsentWithdrawn.builder()
                .all(false)
                .documentDescription("withdrawn event doc description")
                .documentId("withdrawn event doc  id")
                .documentName("withdrawn event doc name")
                .documentVersion("withdrawn event doc version")
                .consentDocuments(documents)
                .build();
    }
}
