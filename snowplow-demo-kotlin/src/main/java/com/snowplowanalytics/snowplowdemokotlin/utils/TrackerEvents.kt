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
package com.snowplowanalytics.snowplowdemokotlin.utils

import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.event.*
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import java.util.*

/**
 * Utility Class used to send all
 * combinations of Tracker Events.
 */
object TrackerEvents {
    fun trackAll(tracker: TrackerController) {
        trackDeepLink(tracker)
        trackPageView(tracker)
        trackStructuredEvent(tracker)
        trackScreenView(tracker)
        trackTimings(tracker)
        trackUnstructuredEvent(tracker)
        trackEcommerceEvent(tracker)
        trackConsentGranted(tracker)
        trackConsentWithdrawn(tracker)
        trackMessageNotification(tracker)
    }

    private fun trackDeepLink(tracker: TrackerController) {
        val event = DeepLinkReceived("http://snowplow.io/path?param=value&param2=value2")
            .referrer("http://snowplow.io/path?param=value&param2=value2")
        tracker.track(event)
    }

    private fun trackPageView(tracker: TrackerController) {
        tracker.track(PageView("pageUrl").pageTitle("pageTitle").referrer("pageReferrer"))
    }

    private fun trackStructuredEvent(tracker: TrackerController) {
        tracker.track(
            Structured("category", "action").label("label").property("property").value(0.00)
        )
    }

    private fun trackScreenView(tracker: TrackerController) {
        tracker.track(ScreenView("screenName1", UUID.randomUUID()))
    }

    private fun trackTimings(tracker: TrackerController) {
        tracker.track(Timing("category", "variable", 1).label("label"))
    }

    private fun trackUnstructuredEvent(tracker: TrackerController) {
        val attributes: MutableMap<String, String> = HashMap()
        attributes["targetUrl"] = "http://a-target-url.com"
        val test = SelfDescribingJson(
            "iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1",
            attributes
        )
        tracker.track(SelfDescribing(test))
    }

    private fun trackEcommerceEvent(tracker: TrackerController) {
        val item = EcommerceTransactionItem("sku-1", 35.00, 1).name("Acme 1").category("Stuff")
            .currency("AUD").orderId("item-1")
        val items: MutableList<EcommerceTransactionItem> = LinkedList()
        items.add(item)
        tracker.track(
            EcommerceTransaction("order-1", 42.50, items).affiliation("affiliation").taxValue(2.50)
                .shipping(5.00).city("Sydney").state("NSW").country("Australia").currency("AUD")
        )
    }

    private fun trackConsentGranted(tracker: TrackerController) {
        val documents: MutableList<ConsentDocument> = LinkedList()
        documents.add(
            ConsentDocument("granted context id 1", "granted context version 1")
                .documentDescription("granted context desc 1")
                .documentName("granted context name 1")
        )
        documents.add(
            ConsentDocument("granted context id 2", "granted context version 2")
                .documentDescription("granted context desc 2")
                .documentName("granted context name 2")
        )
        val event = ConsentGranted(
            "2018-05-08T18:12:02+00:00",
            "granted event doc id",
            "granted event doc version"
        )
            .documentDescription("granted event doc description")
            .documentName("granted event doc name")
            .documents(documents)
        tracker.track(event)
    }

    private fun trackConsentWithdrawn(tracker: TrackerController) {
        val documents: MutableList<ConsentDocument> = LinkedList()
        documents.add(
            ConsentDocument("withdrawn context id 1", "withdrawn context version 1")
                .documentDescription("withdrawn context desc 1")
                .documentName("withdrawn context name 1")
        )
        documents.add(
            ConsentDocument("withdrawn context id 2", "withdrawn context version 2")
                .documentDescription("withdrawn context desc 2")
                .documentName("withdrawn context name 2")
        )
        val event =
            ConsentWithdrawn(false, "withdrawn event doc  id", "withdrawn event doc version")
                .documentDescription("withdrawn event doc description")
                .documentName("withdrawn event doc name")
                .documents(documents)
        tracker.track(event)
    }

    private fun trackMessageNotification(tracker: TrackerController) {
        val event = MessageNotification("title", "body", MessageNotificationTrigger.PUSH)
            .notificationTimestamp("2021-10-18T10:16:08.008Z")
            .category("category")
            .action("action")
            .bodyLocKey("loc key")
            .bodyLocArgs(listOf("loc arg1", "loc arg2"))
            .sound("chime.mp3")
            .notificationCount(9)
            .category("category1")
        tracker.track(event)
    }
}
