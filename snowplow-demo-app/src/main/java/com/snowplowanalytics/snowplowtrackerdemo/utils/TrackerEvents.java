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

package com.snowplowanalytics.snowplowtrackerdemo.utils;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.event.AbstractPrimitive;
import com.snowplowanalytics.snowplow.event.DeepLinkReceived;
import com.snowplowanalytics.snowplow.event.MessageNotification;
import com.snowplowanalytics.snowplow.event.MessageNotificationTrigger;
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

import java.util.Arrays;
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
        trackMessageNotification(tracker);
    }
    
    private static void trackDeepLink(TrackerController tracker) {
        DeepLinkReceived event = new DeepLinkReceived("http://snowplow.io/path?param=value&param2=value2")
                .referrer("http://snowplow.io/path?param=value&param2=value2");
        tracker.track(event);
    }

    private static void trackPageView(TrackerController tracker) {
        tracker.track(new PageView("pageUrl").pageTitle("pageTitle").referrer("pageReferrer"));
    }

    private static void trackStructuredEvent(TrackerController tracker) {
        tracker.track(new Structured("category", "action").label("label").property("property").value(0.00));
    }

    private static void trackScreenView(TrackerController tracker) {
        tracker.track(new ScreenView("screenName1", UUID.randomUUID()));
    }

    private static void trackTimings(TrackerController tracker) {
        tracker.track(new Timing("category","variable", 1).label("label"));
    }

    private static void trackUnstructuredEvent(TrackerController tracker) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("targetUrl", "http://a-target-url.com");
        SelfDescribingJson test = new SelfDescribingJson("iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1", attributes);
        tracker.track(new SelfDescribing(test));
    }

    private static void trackEcommerceEvent(TrackerController tracker) {
        EcommerceTransactionItem item = new EcommerceTransactionItem("sku-1", 35.00, 1).name("Acme 1").category("Stuff").currency("AUD").orderId("item-1");
        List<EcommerceTransactionItem> items = new LinkedList<>();
        items.add(item);
        tracker.track(new EcommerceTransaction("order-1", 42.50, items).affiliation("affiliation").taxValue(2.50).shipping(5.00).city("Sydney").state("NSW").country("Australia").currency("AUD"));
    }

    private static void trackConsentGranted(TrackerController tracker) {
        List<ConsentDocument> documents = new LinkedList<>();
        documents.add(new ConsentDocument("granted context id 1", "granted context version 1")
                .documentDescription("granted context desc 1")
                .documentName("granted context name 1"));
        documents.add(new ConsentDocument("granted context id 2", "granted context version 2")
                .documentDescription("granted context desc 2")
                .documentName("granted context name 2"));
        ConsentGranted event = new ConsentGranted("2018-05-08T18:12:02+00:00", "granted event doc id", "granted event doc version")
                .documentDescription("granted event doc description")
                .documentName("granted event doc name")
                .documents(documents);
        tracker.track(event);
    }

    private static void trackConsentWithdrawn(TrackerController tracker) {
        List<ConsentDocument> documents = new LinkedList<>();
        documents.add(new ConsentDocument("withdrawn context id 1", "withdrawn context version 1")
                .documentDescription("withdrawn context desc 1")
                .documentName("withdrawn context name 1"));
        documents.add(new ConsentDocument("withdrawn context id 2", "withdrawn context version 2")
                .documentDescription("withdrawn context desc 2")
                .documentName("withdrawn context name 2"));
        ConsentWithdrawn event = new ConsentWithdrawn(false, "withdrawn event doc  id", "withdrawn event doc version")
                .documentDescription("withdrawn event doc description")
                .documentName("withdrawn event doc name")
                .documents(documents);
        tracker.track(event);
    }

    private static void trackMessageNotification(TrackerController tracker) {
        MessageNotification event = new MessageNotification("title", "body", MessageNotificationTrigger.push)
                .notificationTimestamp("2021-10-18T10:16:08.008Z")
                .category("category")
                .action("action")
                .bodyLocKey("loc key")
                .bodyLocArgs(Arrays.asList("loc arg1", "loc arg2"))
                .sound("chime.mp3")
                .notificationCount(9)
                .category("category1");
        tracker.track(event);
    }
}
