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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.ecommerce.EcommerceAction
import com.snowplowanalytics.snowplow.ecommerce.events.AddToCartEvent
import com.snowplowanalytics.snowplow.ecommerce.entities.ProductEntity
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class AddToCartTest {
    @Test
    fun testExpectedForm() {
        val product1 = ProductEntity(
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
        val product2 = ProductEntity(
            id = "ID2",
            price = 0.99,
            currency = "GBP",
            category = "category"
        )
        
        var event = AddToCartEvent(totalValue = 123.45, currency = "GBP", products = listOf(product1, product2))
        var cartMap = hashMapOf<String, Any>(
            "schema" to TrackerConstants.SCHEMA_ECOMMERCE_CART,
            "data" to hashMapOf<String, Any>(
                Parameters.ECOMM_CART_VALUE to 123.45,
                Parameters.ECOMM_CART_CURRENCY to "GBP"
            )
        )
        
        val data: Map<String, Any?> = event.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals(EcommerceAction.add_to_cart.toString(), data[Parameters.ECOMM_TYPE])
        Assert.assertFalse(data.containsKey(Parameters.ECOMM_PRODUCTS))
        Assert.assertFalse(data.containsKey(Parameters.ECOMM_NAME))
        Assert.assertFalse(data.containsKey(Parameters.ECOMM_CART_ID))
        
        var entities = event.entitiesForProcessing
        Assert.assertNotNull(entities)
        Assert.assertEquals(3, entities!!.size)
        Assert.assertEquals(cartMap, entities[2].map)
        
        event = AddToCartEvent(listOf(product1), 0.5, "USD", "id")
        cartMap = hashMapOf(
            "schema" to TrackerConstants.SCHEMA_ECOMMERCE_CART,
            "data" to hashMapOf<String, Any>(
                Parameters.ECOMM_CART_VALUE to 0.5,
                Parameters.ECOMM_CART_CURRENCY to "USD",
                Parameters.ECOMM_CART_ID to "id",
            ))
        
        entities = event.entitiesForProcessing
        Assert.assertEquals(cartMap, entities!![1].map)
    }
}
