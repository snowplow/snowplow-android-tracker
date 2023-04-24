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

import android.annotation.SuppressLint
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.ecommerce.EcommerceAction
import com.snowplowanalytics.core.tracker.Tracker
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.TestUtils.createSessionSharedPreferences
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.ecommerce.Product
import com.snowplowanalytics.snowplow.event.*
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.MockNetworkConnection
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@RunWith(AndroidJUnit4::class)
class EcommerceTest {
    
    @Test
    @Throws(Exception::class)
    fun productView() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        Snowplow.removeAllTrackers()
        
        val networkConfig = NetworkConfiguration(networkConnection)
        val trackerConfig = TrackerConfiguration("appId").base64encoding(false)

        val tracker = Snowplow.createTracker(
            context,
            "ns${Math.random()}",
            networkConfig,
            trackerConfig
        )
        
        val product = Product(
            "id", 
            price = 12.34, 
            currency = "GBP", 
            name = "lovely product", 
            position = 1
        )
        tracker.track(ProductView(product))
        
        run {
            var i = 0
            while (i < 10 && networkConnection.countRequests() == 0) {
                Thread.sleep(1000)
                i++
            }
        }
        
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
        
        
        
        
        
        

        
//        val product2 = Product("id2", price = 34.99, currency = "USD", name = "product 2", position = 2)
//        val transaction = Transaction("id", 123, "EUR", "method")
//
//        
//        val productListClick = ProductListClick(product)
//        val productListView = ProductListView(listOf(product, product2))
//        val transactionEvent = TransactionEvent(transaction, listOf(product))
//
////        tracker.track(productView)
////        tracker.track(productListClick)
////        tracker.track(productListView)
////        tracker.track(transactionEvent)
//        tracker.track(ScreenView("screenview"))
//
//        waitForTracker(tracker)

        
    }
    

    // Helpers
    private fun getTracker(namespace: String, uri: String): TrackerController {
        val ns = namespace + Math.random()
            .toString() // add random number to ensure different namespace on each run
        createSessionSharedPreferences(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ns
        )
        val networkConfig = NetworkConfiguration(uri)
        val trackerConfig = TrackerConfiguration("appId").base64encoding(false)

        return Snowplow.createTracker(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ns,
            networkConfig,
            trackerConfig,
//            ecommPlugin
        )
    }

    @SuppressLint("DefaultLocale")
    fun getMockServerURI(mockServer: MockWebServer): String {
        return String.format("%s:%d", mockServer.hostName, mockServer.port)
    }

    @Throws(Exception::class)
    fun getRequests(mockServer: MockWebServer, count: Int): LinkedList<RecordedRequest?> {
        val requests = LinkedList<RecordedRequest?>()
        for (i in 0 until count) {
            val request = mockServer.takeRequest(60, TimeUnit.SECONDS)
            if (request == null) {
                Assert.fail("MockWebServer didn't receive events.")
            }
            requests.add(request)
        }
        return requests
    }
    

    @Throws(Exception::class)
    fun waitForTracker(tracker: TrackerController) {
        var counter = 0
        while (!tracker.emitter.isSending) {
            Thread.sleep(500)
            counter++
            if (counter > 10) {
                return
            }
        }
        counter = 0
        while (tracker.emitter.isSending) {
            Thread.sleep(500)
            counter++
            if (counter > 10) {
                return
            }
        }
        Thread.sleep(500)
        tracker.pause()
    }
    

    @Throws(Exception::class)
    fun checkScreenView(json: JSONObject) {
        Assert.assertEquals("screenId", json.getString("id"))
        Assert.assertEquals("screenName", json.getString("name"))
    }

    @Throws(Exception::class)
    fun checkEvent(json: JSONObject) {
        Assert.assertEquals("screenId", json.getString("id"))
        Assert.assertEquals("screenName", json.getString("name"))
    }

    

    @Throws(Exception::class)
    fun checkUnstructuredEvent(json: JSONObject) {
        val unstructEvent = JSONObject(json.getString("ue_pr"))
        Assert.assertEquals(
            "iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0",
            unstructEvent.getString("schema")
        )
        val innerSchema = unstructEvent.getJSONObject("data").getString("schema")
        val innerData = unstructEvent.getJSONObject("data").getJSONObject("data")
        when (innerSchema) {
            "iglu:com.snowplowanalytics.snowplow/screen_view/jsonschema/1-0-0" -> checkScreenView(
                innerData
            )
            else -> {}
        }
    }
    

    private val customContext: List<SelfDescribingJson>
        get() {
            val contexts: MutableList<SelfDescribingJson> = ArrayList()
            val attributes1: MutableMap<String, String> = HashMap()
            attributes1["key-1"] = "value-1"
            val json1 = SelfDescribingJson(
                "iglu:com.snowplowanalytics.snowplow/example_1/jsonschema/1-0-1", attributes1
            )
            contexts.add(json1)
            return contexts
        }

    companion object {
        private var tracker: Tracker? = null
    }
}
