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
package com.snowplowanalytics.snowplow.event

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.ecommerce.EcommerceAction
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.configuration.Configuration
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.PluginConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.ecommerce.entities.Product
import com.snowplowanalytics.snowplow.ecommerce.events.ProductView
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.tracker.MockNetworkConnection
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class ProductViewTest {
    @Test
    fun testExpectedForm() {
        val product = Product(
            id = "product ID",
            name = "product name",
            category = "category",
            price = 100,
            listPrice = 110,
            quantity = 2,
            size = "small",
            variant = "black/black",
            brand = "Snowplow",
            inventoryStatus = "backorder",
            position = 1,
            currency = "GBP",
            creativeId = "ecomm1"
        )
        val productView = ProductView(product)
        val data: Map<String, Any?> = productView.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals(data[Parameters.ECOMM_TYPE], EcommerceAction.product_view)
        Assert.assertTrue(data.containsKey(Parameters.ECOMM_PRODUCT))
        Assert.assertFalse(data.containsKey(Parameters.ECOMM_NAME))
        Assert.assertEquals(data[Parameters.ECOMM_PRODUCT], product)
    }

    @Test
    fun testEventDetailsConvertedIntoEntity() {
        // plugin to check if event was tracked
        var eventTracked = false
        
        val plugin = PluginConfiguration("testPlugin")
        plugin.afterTrack { 
            eventTracked = true
            println("🔴 " + it.entities)
        }
        
        val tracker = createTracker(listOf(plugin))
        val event = ProductView(Product("id", price = 123, currency = "GBP", category = "cat"))
        tracker.track(event)
        
        Thread.sleep(500)

        // check if event was tracked
        Assert.assertTrue(eventTracked)

    }

    private fun createTracker(configurations: List<Configuration>): TrackerController {
        val networkConfig = NetworkConfiguration(MockNetworkConnection(HttpMethod.POST, 200))
        return Snowplow.createTracker(
            context = context,
            namespace = "ns" + Math.random().toString(),
            network = networkConfig,
            configurations = configurations.toTypedArray()
        )
    }

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
}
