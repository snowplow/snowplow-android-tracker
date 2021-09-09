/*
 * Copyright (c) 2015-2020 Snowplow Analytics Ltd. All rights reserved.
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

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.event.AbstractPrimitive;
import com.snowplowanalytics.snowplow.event.DeepLinkReceived;
import com.snowplowanalytics.snowplow.event.SelfDescribing;
import com.snowplowanalytics.snowplow.event.ConsentDocument;
import com.snowplowanalytics.snowplow.event.ConsentGranted;
import com.snowplowanalytics.snowplow.event.ConsentWithdrawn;
import com.snowplowanalytics.snowplow.event.EcommerceTransaction;
import com.snowplowanalytics.snowplow.event.EcommerceTransactionItem;
import com.snowplowanalytics.snowplow.event.PageView;
import com.snowplowanalytics.snowplow.event.ScreenView;
import com.snowplowanalytics.snowplow.event.Structured;
import com.snowplowanalytics.snowplow.event.Timing;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

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

    public static void trackAll(@NonNull TrackerController tracker) {
        trackDeepLink(tracker);
        trackPageView(tracker);
        trackStructuredEvent(tracker);
        trackScreenView(tracker);
        trackTimings(tracker);
        trackUnstructuredEvent(tracker);
        trackEcommerceEvent(tracker);
        trackConsentGranted(tracker);
        trackConsentWithdrawn(tracker);
    }
    
    private static void trackDeepLink(TrackerController tracker) {
        DeepLinkReceived event = new DeepLinkReceived("url link").referrer("referrer url");
        tracker.track(event);
    }

    private static void trackPageView(TrackerController tracker) {
        tracker.track(PageView.builder().pageUrl("pageUrl").pageTitle("pageTitle").referrer("pageReferrer").build());
    }

    private static void trackStructuredEvent(TrackerController tracker) {
        tracker.track(Structured.builder().category("category").action("action").label("label").property("property").value(0.00).build());
    }

    private static void trackScreenView(TrackerController tracker) {
        tracker.track(ScreenView.builder().name("screenName1").id(UUID.randomUUID().toString()).build());
    }

    private static void trackTimings(TrackerController tracker) {
        tracker.track(Timing.builder().category("category").variable("variable").timing(1).label("label").build());
    }

    private static void trackUnstructuredEvent(TrackerController tracker) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("targetUrl", "http://a-target-url.com");
        SelfDescribingJson test = new SelfDescribingJson("iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1", attributes);
        tracker.track(SelfDescribing.builder().eventData(test).build());
    }

    private static void trackEcommerceEvent(TrackerController tracker) {
        EcommerceTransactionItem item = EcommerceTransactionItem.builder().itemId("item-1").sku("sku-1").price(35.00).quantity(1).name("Acme 1").category("Stuff").currency("AUD").build();
        List<EcommerceTransactionItem> items = new LinkedList<>();
        items.add(item);

        tracker.track(EcommerceTransaction.builder().orderId("order-1").totalValue(42.50).affiliation("affiliation").taxValue(2.50).shipping(5.00).city("Sydney").state("NSW").country("Australia").currency("AUD").items(items).build());
    }

    private static void trackConsentGranted(TrackerController tracker) {
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
        tracker.track(event);
    }

    private static void trackConsentWithdrawn(TrackerController tracker) {
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
        tracker.track(event);
    }
}
