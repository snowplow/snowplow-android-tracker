/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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
import com.snowplowanalytics.snowplow.ecommerce.ErrorType
import com.snowplowanalytics.snowplow.ecommerce.entities.CartEntity
import com.snowplowanalytics.snowplow.ecommerce.entities.ProductEntity
import com.snowplowanalytics.snowplow.ecommerce.entities.PromotionEntity
import com.snowplowanalytics.snowplow.ecommerce.entities.TransactionEntity
import com.snowplowanalytics.snowplow.ecommerce.events.AddToCartEvent
import com.snowplowanalytics.snowplow.ecommerce.events.CheckoutStepEvent
import com.snowplowanalytics.snowplow.ecommerce.events.ProductListClickEvent
import com.snowplowanalytics.snowplow.ecommerce.events.ProductListViewEvent
import com.snowplowanalytics.snowplow.ecommerce.events.ProductViewEvent
import com.snowplowanalytics.snowplow.ecommerce.events.PromotionClickEvent
import com.snowplowanalytics.snowplow.ecommerce.events.PromotionViewEvent
import com.snowplowanalytics.snowplow.ecommerce.events.RefundEvent
import com.snowplowanalytics.snowplow.ecommerce.events.RemoveFromCartEvent
import com.snowplowanalytics.snowplow.ecommerce.events.TransactionErrorEvent
import com.snowplowanalytics.snowplow.ecommerce.events.TransactionEvent
import com.snowplowanalytics.snowplow.event.*
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import java.util.*

/**
 * Utility Class used to send all
 * combinations of Tracker Events.
 */
object TrackerEvents {
    private val product = ProductEntity("productId", "product/category", "GBP", 99.99)
    private val promotion = PromotionEntity("promoIdABCDE")
    private val transaction = TransactionEntity(
        transactionId = "id-123",
        revenue = 231231,
        currency = "USD",
        paymentMethod = "debit",
        totalQuantity = 1
    )
    
    fun trackAll(tracker: TrackerController) {
        trackDeepLink(tracker)
        trackPageView(tracker)
        trackStructuredEvent(tracker)
        trackScreenView(tracker)
        trackTimings(tracker)
        trackSelfDescribingEvent(tracker)
        trackConsentGranted(tracker)
        trackConsentWithdrawn(tracker)
        trackMessageNotification(tracker)

        // Ecommerce events
        trackAddToCart(tracker)
        trackRemoveFromCart(tracker)
        trackCheckoutStep(tracker)
        trackProductView(tracker)
        trackProductListView(tracker)
        trackProductListClick(tracker)
        trackPromotionView(tracker)
        trackPromotionClick(tracker)
        trackTransaction(tracker)
        trackTransactionError(tracker)
        trackRefund(tracker)
    }

    private fun trackDeepLink(tracker: TrackerController) {
        val event = DeepLinkReceived("http://snowplow.io/path?param=value&param2=value2")
            .referrer("http://snowplow.io/path?param=value&param2=value2")

        // add a context entity
        val entity = SelfDescribingJson(
            "iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1",
            mapOf("targetUrl" to "http://a-target-url.com")
        )
        event.entities.add(entity)

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

    private fun trackSelfDescribingEvent(tracker: TrackerController) {
        val attributes: MutableMap<String, String> = HashMap()
        attributes["targetUrl"] = "http://a-target-url.com"
        val test = SelfDescribingJson(
            "iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1",
            attributes
        )
        tracker.track(SelfDescribing(test))
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
        documents.add(
            ConsentDocument("granted context id 3", "granted context version 3")
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
        documents.add(
            ConsentDocument("withdrawn context id 3", "withdrawn context version 3")
        )
        val event =
            ConsentWithdrawn(false, "withdrawn event doc  id", "withdrawn event doc version")
                .documentDescription("withdrawn event doc description")
                .documentName("withdrawn event doc name")
                .documents(documents)
        tracker.track(event)
    }

    private fun trackMessageNotification(tracker: TrackerController) {
        val event = MessageNotification("title", "body", MessageNotificationTrigger.push)
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

    private fun trackAddToCart(tracker: TrackerController) {
        val event = AddToCartEvent(listOf(product), CartEntity(currency = "GBP", totalValue = 123.45))
        tracker.track(event)
    }

    private fun trackRemoveFromCart(tracker: TrackerController) {
        val event = RemoveFromCartEvent(listOf(product), CartEntity(currency = "GBP", totalValue = 43.21))
        tracker.track(event)
    }

    private fun trackCheckoutStep(tracker: TrackerController) {
        val event = CheckoutStepEvent(3, accountType = "guest")
        tracker.track(event)
    }

    private fun trackProductView(tracker: TrackerController) {
        val event = ProductViewEvent(product)
        tracker.track(event)
    }

    private fun trackProductListView(tracker: TrackerController) {
        val event = ProductListViewEvent(listOf(product), "snowplowProducts")
        tracker.track(event)
    }

    private fun trackProductListClick(tracker: TrackerController) {
        val event = ProductListClickEvent(product, "snowplowProducts")
        tracker.track(event)
    }

    private fun trackPromotionView(tracker: TrackerController) {
        val event = PromotionViewEvent(promotion)
        tracker.track(event)
    }

    private fun trackPromotionClick(tracker: TrackerController) {
        val event = PromotionClickEvent(promotion)
        tracker.track(event)
    }

    private fun trackTransaction(tracker: TrackerController) {
        val event = TransactionEvent(transaction)
        tracker.track(event)
    }

    private fun trackTransactionError(tracker: TrackerController) {
        val event = TransactionErrorEvent(
            transaction,
            errorShortcode = "processor_declined",
            errorDescription = "user_details_invalid",
            errorType = ErrorType.Hard
        )
        tracker.track(event)
    }

    private fun trackRefund(tracker: TrackerController) {
        val event = RefundEvent(
            transactionId = "id-123",
            7654321,
            "USD", 
            products = listOf(product)
        )
        tracker.track(event)
    }
}
