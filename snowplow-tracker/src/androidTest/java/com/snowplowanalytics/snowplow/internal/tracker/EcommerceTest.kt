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
import com.snowplowanalytics.snowplow.ecommerce.Product
import com.snowplowanalytics.snowplow.event.*
import com.snowplowanalytics.snowplow.network.HttpMethod
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
        
        val product = Product(
            "id", 
            price = 12.34, 
            currency = "GBP", 
            name = "lovely product", 
            position = 1
        )
        tracker.track(ProductView(product))
        waitForEvents(networkConnection, 1)
        
        Assert.assertEquals(1, networkConnection.countRequests())
        
        val request = networkConnection.allRequests[0]
        val event = JSONObject(request.payload.map["ue_pr"] as String)
            .getJSONObject("data")
        val entities = JSONObject(request.payload.map["co"] as String)
            .getJSONArray("data")
        
        var productEntity: JSONObject? = null
        for (i in 0 until entities.length()) {
            if (entities.getJSONObject(i).getString("schema") == TrackerConstants.SCHEMA_ECOMMERCE_PRODUCT) {
                productEntity = entities.getJSONObject(i).getJSONObject("data")
            }
        }
        Assert.assertEquals(TrackerConstants.SCHEMA_ECOMMERCE_ACTION, event.getString("schema"))
        Assert.assertEquals(EcommerceAction.product_view.toString(), event.getJSONObject("data").getString("type"))
        Assert.assertFalse(event.getJSONObject("data").has(Parameters.ECOMM_NAME))
        
        Assert.assertNotNull(productEntity)
        Assert.assertEquals("id", productEntity!!.get(Parameters.ECOMM_PRODUCT_ID))
        Assert.assertEquals(12.34, productEntity.get(Parameters.ECOMM_PRODUCT_PRICE))
        Assert.assertEquals("GBP", productEntity.get(Parameters.ECOMM_PRODUCT_CURRENCY))
        Assert.assertEquals("lovely product", productEntity.get(Parameters.ECOMM_PRODUCT_NAME))
        Assert.assertEquals(1, productEntity.get(Parameters.ECOMM_PRODUCT_POSITION))
        
        Assert.assertFalse(productEntity.has(Parameters.ECOMM_PRODUCT_LIST_PRICE))
        Assert.assertFalse(productEntity.has(Parameters.ECOMM_PRODUCT_VARIANT))
    }

    @Test
    @Throws(Exception::class)
    fun productListClick() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val tracker = getTracker(networkConnection)

        val product = Product(
            "id",
            price = 5.00,
            currency = "EUR",
        )
        tracker.track(ProductListClick(product))
        waitForEvents(networkConnection, 1)

        Assert.assertEquals(1, networkConnection.countRequests())

        var request = networkConnection.allRequests[0]
        var event = JSONObject(request.payload.map["ue_pr"] as String)
            .getJSONObject("data")
        val entities = JSONObject(request.payload.map["co"] as String)
            .getJSONArray("data")

        var productEntity: JSONObject? = null
        for (i in 0 until entities.length()) {
            if (entities.getJSONObject(i).getString("schema") == TrackerConstants.SCHEMA_ECOMMERCE_PRODUCT) {
                productEntity = entities.getJSONObject(i).getJSONObject("data")
            }
        }
        Assert.assertEquals(TrackerConstants.SCHEMA_ECOMMERCE_ACTION, event.getString("schema"))
        Assert.assertEquals(EcommerceAction.list_click.toString(), event.getJSONObject("data").getString("type"))
        Assert.assertFalse(event.getJSONObject("data").has(Parameters.ECOMM_NAME))

        Assert.assertNotNull(productEntity)
        Assert.assertEquals("id", productEntity!!.get(Parameters.ECOMM_PRODUCT_ID))
        Assert.assertEquals(5.00, productEntity.get(Parameters.ECOMM_PRODUCT_PRICE))
        Assert.assertEquals("EUR", productEntity.get(Parameters.ECOMM_PRODUCT_CURRENCY))
        Assert.assertFalse(productEntity.has(Parameters.ECOMM_PRODUCT_POSITION))

        tracker.track(ProductListClick(product, "list name"))
        waitForEvents(networkConnection, 2)
        
        request = networkConnection.allRequests[1]
        event = JSONObject(request.payload.map["ue_pr"] as String)
            .getJSONObject("data")
        Assert.assertEquals("list name", event.getJSONObject("data").getString("name"))
    }

    @Test
    @Throws(Exception::class)
    fun productListView() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val tracker = getTracker(networkConnection)

        val product1 = Product(
            "id",
            price = 0.99,
            currency = "CAD",
        )
        val product2 = Product(
            "id2",
            price = 1000,
            currency = "AUD",
            name = "snowplow",
            category = "plows",
            listPrice = 1500.0,
            size = "large",
            variant = "shiny",
            inventoryStatus = "in_stock",
            creativeId = "plow_promo"
        )
        
        tracker.track(ProductListView(listOf(product1, product2), "specials"))
        waitForEvents(networkConnection, 1)

        Assert.assertEquals(1, networkConnection.countRequests())

        val request = networkConnection.allRequests[0]
        val event = JSONObject(request.payload.map["ue_pr"] as String)
            .getJSONObject("data")
        val entities = JSONObject(request.payload.map["co"] as String)
            .getJSONArray("data")

        val productEntities = ArrayList<JSONObject>()
        for (i in 0 until entities.length()) {
            if (entities.getJSONObject(i).getString("schema") == TrackerConstants.SCHEMA_ECOMMERCE_PRODUCT) {
                productEntities.add(entities.getJSONObject(i).getJSONObject("data"))
            }
        }
        
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

        val product1 = Product(
            "id1",
            price = 0.99,
            currency = "USD",
        )

        tracker.track(AddToCart(listOf(product1), 100, "GBP"))
        waitForEvents(networkConnection, 1)

        Assert.assertEquals(1, networkConnection.countRequests())

        val request = networkConnection.allRequests[0]
        val event = JSONObject(request.payload.map["ue_pr"] as String)
            .getJSONObject("data")
        val entities = JSONObject(request.payload.map["co"] as String)
            .getJSONArray("data")

        val productEntities = ArrayList<JSONObject>()
        val cartEntities = ArrayList<JSONObject>()
        for (i in 0 until entities.length()) {
            if (entities.getJSONObject(i).getString("schema") == TrackerConstants.SCHEMA_ECOMMERCE_PRODUCT) {
                productEntities.add(entities.getJSONObject(i).getJSONObject("data"))
            } else if (entities.getJSONObject(i).getString("schema") == TrackerConstants.SCHEMA_ECOMMERCE_CART) {
                cartEntities.add(entities.getJSONObject(i).getJSONObject("data"))
            }
        }

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
}
