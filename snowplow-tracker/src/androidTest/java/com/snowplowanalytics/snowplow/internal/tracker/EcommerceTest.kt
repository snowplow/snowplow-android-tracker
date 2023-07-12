/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.internal.tracker

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.ecommerce.EcommerceAction
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.ecommerce.entities.CartEntity
import com.snowplowanalytics.snowplow.ecommerce.entities.EcommerceScreenEntity
import com.snowplowanalytics.snowplow.ecommerce.entities.EcommerceUserEntity
import com.snowplowanalytics.snowplow.ecommerce.events.AddToCartEvent
import com.snowplowanalytics.snowplow.ecommerce.entities.ProductEntity
import com.snowplowanalytics.snowplow.ecommerce.entities.PromotionEntity
import com.snowplowanalytics.snowplow.ecommerce.entities.TransactionEntity
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
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.network.Request
import com.snowplowanalytics.snowplow.tracker.MockNetworkConnection
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.collections.ArrayList

@RunWith(AndroidJUnit4::class)
class EcommerceTest {
    
    @Test
    @Throws(Exception::class)
    fun productView() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val tracker = getTracker(networkConnection)
        
        val product = ProductEntity(
            "id", 
            price = 12.34, 
            currency = "GBP", 
            name = "lovely product", 
            position = 1,
            category = "accessories"
        )
        tracker.track(ProductViewEvent(product))
        waitForEvents(networkConnection, 1)
        
        Assert.assertEquals(1, networkConnection.countRequests())
        
        val request = networkConnection.allRequests[0]
        val event = getEvent(request)
        val productEntities = getProductEntities(request)
        
        Assert.assertEquals(TrackerConstants.SCHEMA_ECOMMERCE_ACTION, event.getString("schema"))
        Assert.assertEquals(EcommerceAction.product_view.toString(), event.getJSONObject("data").getString("type"))
        Assert.assertFalse(event.getJSONObject("data").has(Parameters.ECOMM_NAME))
        
        Assert.assertEquals(1, productEntities.size)
        Assert.assertEquals("id", productEntities[0].get(Parameters.ECOMM_PRODUCT_ID))
        Assert.assertEquals(12.34, productEntities[0].get(Parameters.ECOMM_PRODUCT_PRICE))
        Assert.assertEquals("GBP", productEntities[0].get(Parameters.ECOMM_PRODUCT_CURRENCY))
        Assert.assertEquals("lovely product", productEntities[0].get(Parameters.ECOMM_PRODUCT_NAME))
        Assert.assertEquals(1, productEntities[0].get(Parameters.ECOMM_PRODUCT_POSITION))
        
        Assert.assertFalse(productEntities[0].has(Parameters.ECOMM_PRODUCT_LIST_PRICE))
        Assert.assertFalse(productEntities[0].has(Parameters.ECOMM_PRODUCT_VARIANT))
    }

    @Test
    @Throws(Exception::class)
    fun productListClick() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val tracker = getTracker(networkConnection)

        val product = ProductEntity(
            "id",
            price = 5.00,
            currency = "EUR",
            category = "misc"
        )
        tracker.track(ProductListClickEvent(product))
        waitForEvents(networkConnection, 1)

        Assert.assertEquals(1, networkConnection.countRequests())

        var request = networkConnection.allRequests[0]
        var event = getEvent(request)
        val productEntities = getProductEntities(request)
        
        Assert.assertEquals(TrackerConstants.SCHEMA_ECOMMERCE_ACTION, event.getString("schema"))
        Assert.assertEquals(EcommerceAction.list_click.toString(), event.getJSONObject("data").getString("type"))
        Assert.assertFalse(event.getJSONObject("data").has(Parameters.ECOMM_NAME))

        Assert.assertEquals(1, productEntities.size)
        Assert.assertEquals("id", productEntities[0].get(Parameters.ECOMM_PRODUCT_ID))
        Assert.assertEquals(5, productEntities[0].get(Parameters.ECOMM_PRODUCT_PRICE))
        Assert.assertEquals("EUR", productEntities[0].get(Parameters.ECOMM_PRODUCT_CURRENCY))
        Assert.assertFalse(productEntities[0].has(Parameters.ECOMM_PRODUCT_POSITION))

        tracker.track(ProductListClickEvent(product, "list name"))
        waitForEvents(networkConnection, 2)
        
        request = networkConnection.allRequests[1]
        event = getEvent(request)
        Assert.assertEquals("list name", event.getJSONObject("data").getString("name"))
    }

    @Test
    @Throws(Exception::class)
    fun productListView() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val tracker = getTracker(networkConnection)

        val product1 = ProductEntity(
            "id",
            price = 0.99,
            currency = "CAD",
            category = "motors"
        )
        val product2 = ProductEntity(
            "id2",
            price = 1000,
            currency = "AUD",
            name = "snowplow",
            category = "ploughs",
            listPrice = 1500.0,
            size = "large",
            variant = "shiny",
            inventoryStatus = "in_stock",
            creativeId = "plow_promo"
        )
        
        tracker.track(ProductListViewEvent(listOf(product1, product2), "specials"))
        waitForEvents(networkConnection, 1)

        Assert.assertEquals(1, networkConnection.countRequests())

        val request = networkConnection.allRequests[0]
        val event = getEvent(request)
        val productEntities = getProductEntities(request)
        
        Assert.assertEquals(TrackerConstants.SCHEMA_ECOMMERCE_ACTION, event.getString("schema"))
        Assert.assertEquals(EcommerceAction.list_view.toString(), event.getJSONObject("data").getString("type"))
        Assert.assertEquals("specials", event.getJSONObject("data").getString("name"))

        Assert.assertEquals(2, productEntities.size)
        
        var entity = productEntities[0]
        Assert.assertEquals("id", entity.get(Parameters.ECOMM_PRODUCT_ID))
        Assert.assertEquals(0.99, entity.get(Parameters.ECOMM_PRODUCT_PRICE))
        Assert.assertEquals("CAD", entity.get(Parameters.ECOMM_PRODUCT_CURRENCY))
        Assert.assertFalse(entity.has(Parameters.ECOMM_PRODUCT_NAME))

        entity = productEntities[1]
        Assert.assertEquals("id2", entity.get(Parameters.ECOMM_PRODUCT_ID))
        Assert.assertEquals(1000, entity.get(Parameters.ECOMM_PRODUCT_PRICE))
        Assert.assertEquals("AUD", entity.get(Parameters.ECOMM_PRODUCT_CURRENCY))
        Assert.assertEquals("large", entity.get(Parameters.ECOMM_PRODUCT_SIZE))
        Assert.assertEquals("shiny", entity.get(Parameters.ECOMM_PRODUCT_VARIANT))
    }

    @Test
    @Throws(Exception::class)
    fun addToCart() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val tracker = getTracker(networkConnection)

        val product1 = ProductEntity(
            "id1",
            price = 0.99,
            category = "flour",
            currency = "USD",
        )

        tracker.track(AddToCartEvent(listOf(product1), CartEntity(100, "GBP")))
        waitForEvents(networkConnection, 1)

        Assert.assertEquals(1, networkConnection.countRequests())

        val request = networkConnection.allRequests[0]
        val event = getEvent(request)
        val productEntities = getProductEntities(request)
        val cartEntities = getCartEntities(request)

        Assert.assertEquals(TrackerConstants.SCHEMA_ECOMMERCE_ACTION, event.getString("schema"))
        Assert.assertEquals(EcommerceAction.add_to_cart.toString(), event.getJSONObject("data").getString("type"))
        Assert.assertFalse(event.getJSONObject("data").has(Parameters.ECOMM_NAME))

        Assert.assertEquals(1, productEntities.size)
        Assert.assertEquals(1, cartEntities.size)

        Assert.assertEquals("id1", productEntities[0].get(Parameters.ECOMM_PRODUCT_ID))

        val cart = cartEntities[0]
        Assert.assertEquals(100, cart.get(Parameters.ECOMM_CART_VALUE))
        Assert.assertEquals("GBP", cart.get(Parameters.ECOMM_CART_CURRENCY))
        Assert.assertFalse(cart.has(Parameters.ECOMM_CART_ID))
    }

    @Test
    @Throws(Exception::class)
    fun removeFromCart() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val tracker = getTracker(networkConnection)

        val product = ProductEntity(
            "product123",
            price = 200000,
            currency = "JPY",
            category = "kitchen"
        )

        tracker.track(RemoveFromCartEvent(listOf(product), CartEntity(400000, "JPY", "cart567")))
        waitForEvents(networkConnection, 1)

        Assert.assertEquals(1, networkConnection.countRequests())

        val request = networkConnection.allRequests[0]
        val event = getEvent(request)
        val productEntities = getProductEntities(request)
        val cartEntities = getCartEntities(request)

        Assert.assertEquals(TrackerConstants.SCHEMA_ECOMMERCE_ACTION, event.getString("schema"))
        Assert.assertEquals(EcommerceAction.remove_from_cart.toString(), event.getJSONObject("data").getString("type"))
        Assert.assertFalse(event.getJSONObject("data").has(Parameters.ECOMM_NAME))

        Assert.assertEquals(1, productEntities.size)
        Assert.assertEquals(1, cartEntities.size)

        Assert.assertEquals("product123", productEntities[0].get(Parameters.ECOMM_PRODUCT_ID))

        val cart = cartEntities[0]
        Assert.assertEquals(400000, cart.get(Parameters.ECOMM_CART_VALUE))
        Assert.assertEquals("JPY", cart.get(Parameters.ECOMM_CART_CURRENCY))
        Assert.assertEquals("cart567", cart.get(Parameters.ECOMM_CART_ID))
    }

    @Test
    @Throws(Exception::class)
    fun checkoutStep() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val tracker = getTracker(networkConnection)

        tracker.track(CheckoutStepEvent(step = 1, couponCode = "WELCOME2023"))
        waitForEvents(networkConnection, 1)

        Assert.assertEquals(1, networkConnection.countRequests())

        val request = networkConnection.allRequests[0]
        val event = getEvent(request)
        val checkoutEntities = getCheckoutEntities(request)

        Assert.assertEquals(TrackerConstants.SCHEMA_ECOMMERCE_ACTION, event.getString("schema"))
        Assert.assertEquals(EcommerceAction.checkout_step.toString(), event.getJSONObject("data").getString("type"))
        Assert.assertFalse(event.getJSONObject("data").has(Parameters.ECOMM_NAME))

        Assert.assertEquals(1, checkoutEntities.size)

        Assert.assertEquals(1, checkoutEntities[0].get(Parameters.ECOMM_CHECKOUT_STEP))
        Assert.assertEquals("WELCOME2023", checkoutEntities[0].get(Parameters.ECOMM_CHECKOUT_COUPON_CODE))
    }

    @Test
    @Throws(Exception::class)
    fun transaction() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val tracker = getTracker(networkConnection)

        val product1 = ProductEntity("id1", currency = "CHF", price = 10.99, category = "climbing")
        val product2 = ProductEntity("id2", currency = "CHF", price = 4, category = "boxing")
        
        val transaction = TransactionEntity(
            transactionId = "id123",
            revenue = 5,
            currency = "CHF",
            paymentMethod = "credit_card",
            totalQuantity = 2
        )

        tracker.track(TransactionEvent(transaction, products = listOf(product1, product2)))
        waitForEvents(networkConnection, 1)

        Assert.assertEquals(1, networkConnection.countRequests())

        val request = networkConnection.allRequests[0]
        val event = getEvent(request)
        val productEntities = getProductEntities(request)
        val transactionEntities = getTransactionEntities(request)

        Assert.assertEquals(TrackerConstants.SCHEMA_ECOMMERCE_ACTION, event.getString("schema"))
        Assert.assertEquals(EcommerceAction.transaction.toString(), event.getJSONObject("data").getString("type"))
        Assert.assertFalse(event.getJSONObject("data").has(Parameters.ECOMM_NAME))

        Assert.assertEquals(2, productEntities.size)
        Assert.assertEquals(1, transactionEntities.size)

        Assert.assertEquals("id1", productEntities[0].get(Parameters.ECOMM_PRODUCT_ID))

        val entity = transactionEntities[0]
        Assert.assertEquals(5, entity.get(Parameters.ECOMM_TRANSACTION_REVENUE))
        Assert.assertEquals("CHF", entity.get(Parameters.ECOMM_TRANSACTION_CURRENCY))
        Assert.assertEquals("id123", entity.get(Parameters.ECOMM_TRANSACTION_ID))
        Assert.assertEquals("credit_card", entity.get(Parameters.ECOMM_TRANSACTION_PAYMENT_METHOD))
    }

    @Test
    @Throws(Exception::class)
    fun transactionError() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val tracker = getTracker(networkConnection)

        val transaction = TransactionEntity(
            transactionId = "id123",
            revenue = 5,
            currency = "CHF",
            paymentMethod = "credit_card",
            totalQuantity = 2
        )
        
        val errorEvent = TransactionErrorEvent(
            transaction,
            "code",
            "processor_declined",
            "user_details_invalid"
        )

        tracker.track(errorEvent)
        waitForEvents(networkConnection, 1)

        Assert.assertEquals(1, networkConnection.countRequests())

        val request = networkConnection.allRequests[0]
        val event = getEvent(request)
        val errorEntities = getTransactionErrorEntities(request)
        val transactionEntities = getTransactionEntities(request)

        Assert.assertEquals(TrackerConstants.SCHEMA_ECOMMERCE_ACTION, event.getString("schema"))
        Assert.assertEquals(EcommerceAction.trns_error.toString(), event.getJSONObject("data").getString("type"))
        Assert.assertFalse(event.getJSONObject("data").has(Parameters.ECOMM_NAME))

        Assert.assertEquals(1, errorEntities.size)
        Assert.assertEquals(1, transactionEntities.size)

        val entity = errorEntities[0]
        Assert.assertEquals("code", entity.get(Parameters.ECOMM_TRANSACTION_ERROR_CODE))
        Assert.assertEquals("processor_declined", entity.get(Parameters.ECOMM_TRANSACTION_ERROR_SHORTCODE))
        Assert.assertEquals("user_details_invalid", entity.get(Parameters.ECOMM_TRANSACTION_ERROR_DESCRIPTION))
    }

    @Test
    @Throws(Exception::class)
    fun promotionView() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val tracker = getTracker(networkConnection)

        val promo = PromotionEntity(
            "promo1",
            productIds = listOf("product1", "product2", "product3"),
        )

        tracker.track(PromotionViewEvent(promo))
        waitForEvents(networkConnection, 1)

        Assert.assertEquals(1, networkConnection.countRequests())

        val request = networkConnection.allRequests[0]
        val event = getEvent(request)
        val promoEntities = getPromoEntities(request)

        Assert.assertEquals(TrackerConstants.SCHEMA_ECOMMERCE_ACTION, event.getString("schema"))
        Assert.assertEquals(EcommerceAction.promo_view.toString(), event.getJSONObject("data").getString("type"))
        Assert.assertFalse(event.getJSONObject("data").has(Parameters.ECOMM_NAME))

        Assert.assertEquals(1, promoEntities.size)

        Assert.assertEquals("promo1", promoEntities[0].get(Parameters.ECOMM_PROMO_ID))
        Assert.assertEquals("[\"product1\",\"product2\",\"product3\"]", promoEntities[0].get(Parameters.ECOMM_PROMO_PRODUCT_IDS).toString())
    }

    @Test
    @Throws(Exception::class)
    fun promotionClick() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val tracker = getTracker(networkConnection)

        val promo = PromotionEntity(
            "promo1",
        )

        tracker.track(PromotionClickEvent(promo))
        waitForEvents(networkConnection, 1)

        Assert.assertEquals(1, networkConnection.countRequests())

        val request = networkConnection.allRequests[0]
        println(request.payload)
        val event = getEvent(request)
        val promoEntities = getPromoEntities(request)

        Assert.assertEquals(TrackerConstants.SCHEMA_ECOMMERCE_ACTION, event.getString("schema"))
        Assert.assertEquals(EcommerceAction.promo_click.toString(), event.getJSONObject("data").getString("type"))

        Assert.assertEquals(1, promoEntities.size)

        Assert.assertEquals("promo1", promoEntities[0].get(Parameters.ECOMM_PROMO_ID))
    }

    @Test
    @Throws(Exception::class)
    fun refund() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val tracker = getTracker(networkConnection)
        
        val product1 = ProductEntity("productId", category = "roses", price = 24.99, currency = "CAD")
        val product2 = ProductEntity("id2", category = "gloves", price = 3.56, currency = "CAD")
        
        tracker.track(RefundEvent("refund_123",
            currency = "GBP",
            refundAmount = 1.00,
            products = listOf(product1, product2))
        )
        waitForEvents(networkConnection, 1)
        
        Assert.assertEquals(1, networkConnection.countRequests())

        val request = networkConnection.allRequests[0]
        val event = getEvent(request)
        val productEntities = getProductEntities(request)
        val refundEntities = getRefundEntities(request)

        Assert.assertEquals(TrackerConstants.SCHEMA_ECOMMERCE_ACTION, event.getString("schema"))
        Assert.assertEquals(EcommerceAction.refund.toString(), event.getJSONObject("data").getString("type"))

        Assert.assertEquals(1, refundEntities.size)
        Assert.assertEquals(2, productEntities.size)

        Assert.assertEquals("refund_123", refundEntities[0].get(Parameters.ECOMM_REFUND_ID))
        Assert.assertEquals("GBP", refundEntities[0].get(Parameters.ECOMM_REFUND_CURRENCY))
        Assert.assertEquals(1, refundEntities[0].get(Parameters.ECOMM_REFUND_AMOUNT))
        Assert.assertFalse(event.getJSONObject("data").has(Parameters.ECOMM_REFUND_REASON))
    }
    
    @Test
    fun addsPageEntity() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val tracker = getTracker(networkConnection)
        
        tracker.ecommerce.setEcommerceScreen(EcommerceScreenEntity(type = "listing", language = "DE", locale = "DE"))
        
        tracker.track(ScreenView("screen"))
        waitForEvents(networkConnection, 1)
        
        var request = networkConnection.allRequests[0]
        var pageEntities = getPageEntities(request)

        Assert.assertEquals(1, pageEntities.size)
        Assert.assertEquals("DE", pageEntities[0].get("language"))
        Assert.assertEquals("listing", pageEntities[0].get("type"))
        Assert.assertEquals("DE", pageEntities[0].get("locale"))


        // replacing earlier Page
        tracker.ecommerce.setEcommerceScreen(EcommerceScreenEntity(type = "home_screen", language = "EN-GB"))

        tracker.track(Structured("category", "action"))
        waitForEvents(networkConnection, 2)

        request = networkConnection.allRequests[1]
        pageEntities = getPageEntities(request)
        
        Assert.assertEquals(1, pageEntities.size)
        Assert.assertEquals("EN-GB", pageEntities[0].get("language"))
        Assert.assertEquals("home_screen", pageEntities[0].get("type"))
        Assert.assertFalse(pageEntities[0].has("locale"))
        
        // removing Page
        tracker.ecommerce.removeEcommerceScreen()
        tracker.track(ScreenView("productA"))
        waitForEvents(networkConnection, 3)

        request = networkConnection.allRequests[2]
        pageEntities = getPageEntities(request)

        Assert.assertEquals(0, pageEntities.size)
    }

    @Test
    fun addsUserEntity() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val tracker = getTracker(networkConnection)

        tracker.ecommerce.setEcommerceUser(EcommerceUserEntity("user_id"))

        tracker.track(ScreenView("screen"))
        waitForEvents(networkConnection, 1)

        var request = networkConnection.allRequests[0]
        var userEntities = getUserEntities(request)

        Assert.assertEquals(1, userEntities.size)
        Assert.assertEquals("user_id", userEntities[0].get("id"))
        Assert.assertFalse(userEntities[0].has("is_guest"))

        // replacing earlier User
        tracker.ecommerce.setEcommerceUser(EcommerceUserEntity("a_new_user", false, "email@email.com"))

        tracker.track(Structured("category", "action"))
        waitForEvents(networkConnection, 2)

        request = networkConnection.allRequests[1]
        userEntities = getUserEntities(request)

        Assert.assertEquals(1, userEntities.size)
        Assert.assertEquals("a_new_user", userEntities[0].get("id"))
        Assert.assertEquals(false, userEntities[0].get("is_guest"))
        Assert.assertEquals("email@email.com", userEntities[0].get("email"))

        // removing User
        tracker.ecommerce.removeEcommerceUser()
        tracker.track(ScreenView("productA"))
        waitForEvents(networkConnection, 3)

        request = networkConnection.allRequests[2]
        userEntities = getUserEntities(request)

        Assert.assertEquals(0, userEntities.size)
    }
    

    // Helpers
    private fun getTracker(networkConnection: MockNetworkConnection): TrackerController {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        val networkConfig = NetworkConfiguration(networkConnection)
        val trackerConfig = TrackerConfiguration("appId").base64encoding(false)

        Snowplow.removeAllTrackers()
        return Snowplow.createTracker(
            context,
            "ns${Math.random()}",
            networkConfig,
            trackerConfig
        )
    }

    @Throws(Exception::class)
    fun waitForEvents(networkConnection: MockNetworkConnection, eventsExpected: Int) {
        var i = 0
        while (i < 10 && networkConnection.countRequests() == eventsExpected - 1) {
            Thread.sleep(1000)
            i++
        }
    }

    private fun getEvent(request: Request) : JSONObject {
        return JSONObject(request.payload.map["ue_pr"] as String)
            .getJSONObject("data")
    }

    private fun getProductEntities(request: Request) : ArrayList<JSONObject> {
        val relevantEntities = ArrayList<JSONObject>()
        val allEntities = JSONObject(request.payload.map["co"] as String)
            .getJSONArray("data")

        for (i in 0 until allEntities.length()) {
            if (allEntities.getJSONObject(i).getString("schema") == TrackerConstants.SCHEMA_ECOMMERCE_PRODUCT) {
                relevantEntities.add(allEntities.getJSONObject(i).getJSONObject("data"))
            }
        }
        return relevantEntities
    }

    private fun getCartEntities(request: Request) : ArrayList<JSONObject> {
        val relevantEntities = ArrayList<JSONObject>()
        val allEntities = JSONObject(request.payload.map["co"] as String)
            .getJSONArray("data")

        for (i in 0 until allEntities.length()) {
            if (allEntities.getJSONObject(i).getString("schema") == TrackerConstants.SCHEMA_ECOMMERCE_CART) {
                relevantEntities.add(allEntities.getJSONObject(i).getJSONObject("data"))
            }
        }
        return relevantEntities
    }

    private fun getCheckoutEntities(request: Request) : ArrayList<JSONObject> {
        val relevantEntities = ArrayList<JSONObject>()
        val allEntities = JSONObject(request.payload.map["co"] as String)
            .getJSONArray("data")

        for (i in 0 until allEntities.length()) {
            if (allEntities.getJSONObject(i).getString("schema") == TrackerConstants.SCHEMA_ECOMMERCE_CHECKOUT_STEP) {
                relevantEntities.add(allEntities.getJSONObject(i).getJSONObject("data"))
            }
        }
        return relevantEntities
    }

    private fun getPromoEntities(request: Request) : ArrayList<JSONObject> {
        val relevantEntities = ArrayList<JSONObject>()
        val allEntities = JSONObject(request.payload.map["co"] as String)
            .getJSONArray("data")

        for (i in 0 until allEntities.length()) {
            if (allEntities.getJSONObject(i).getString("schema") == TrackerConstants.SCHEMA_ECOMMERCE_PROMOTION) {
                relevantEntities.add(allEntities.getJSONObject(i).getJSONObject("data"))
            }
        }
        return relevantEntities
    }

    private fun getTransactionEntities(request: Request) : ArrayList<JSONObject> {
        val relevantEntities = ArrayList<JSONObject>()
        val allEntities = JSONObject(request.payload.map["co"] as String)
            .getJSONArray("data")

        for (i in 0 until allEntities.length()) {
            if (allEntities.getJSONObject(i).getString("schema") == TrackerConstants.SCHEMA_ECOMMERCE_TRANSACTION) {
                relevantEntities.add(allEntities.getJSONObject(i).getJSONObject("data"))
            }
        }
        return relevantEntities
    }

    private fun getTransactionErrorEntities(request: Request) : ArrayList<JSONObject> {
        val relevantEntities = ArrayList<JSONObject>()
        val allEntities = JSONObject(request.payload.map["co"] as String)
            .getJSONArray("data")

        for (i in 0 until allEntities.length()) {
            if (allEntities.getJSONObject(i).getString("schema") == TrackerConstants.SCHEMA_ECOMMERCE_TRANSACTION_ERROR) {
                relevantEntities.add(allEntities.getJSONObject(i).getJSONObject("data"))
            }
        }
        return relevantEntities
    }

    private fun getRefundEntities(request: Request) : ArrayList<JSONObject> {
        val relevantEntities = ArrayList<JSONObject>()
        val allEntities = JSONObject(request.payload.map["co"] as String)
            .getJSONArray("data")

        for (i in 0 until allEntities.length()) {
            if (allEntities.getJSONObject(i).getString("schema") == TrackerConstants.SCHEMA_ECOMMERCE_REFUND) {
                relevantEntities.add(allEntities.getJSONObject(i).getJSONObject("data"))
            }
        }
        return relevantEntities
    }
    
    private fun getPageEntities(request: Request) : ArrayList<JSONObject> {
        val pageEntities = ArrayList<JSONObject>()
        val entities = JSONObject(request.payload.map["co"] as String)
            .getJSONArray("data")

        for (i in 0 until entities.length()) {
            if (entities.getJSONObject(i).getString("schema") == TrackerConstants.SCHEMA_ECOMMERCE_PAGE) {
                pageEntities.add(entities.getJSONObject(i).getJSONObject("data"))
            }
        }
        return pageEntities
    }

    private fun getUserEntities(request: Request) : ArrayList<JSONObject> {
        val userEntities = ArrayList<JSONObject>()
        val entities = JSONObject(request.payload.map["co"] as String)
            .getJSONArray("data")

        for (i in 0 until entities.length()) {
            if (entities.getJSONObject(i).getString("schema") == TrackerConstants.SCHEMA_ECOMMERCE_USER) {
                userEntities.add(entities.getJSONObject(i).getJSONObject("data"))
            }
        }
        return userEntities
    }
}
