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
import com.snowplowanalytics.core.ecommerce.EcommerceAction
import com.snowplowanalytics.snowplow.ecommerce.Product
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class RemoveFromCartTest {
    @Test
    fun testExpectedForm() {
        val product1 = Product(
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
        val product2 = Product(
            id = "ID2",
            price = 0.99,
            currency = "GBP"
        )
        
        var event = RemoveFromCart(totalValue = 123.45, currency = "GBP", products = listOf(product1, product2))
        var data: Map<String, Any?> = event.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals(data[Parameters.ECOMM_TYPE], EcommerceAction.remove_from_cart)
        Assert.assertTrue(data.containsKey(Parameters.ECOMM_PRODUCTS))
        Assert.assertFalse(data.containsKey(Parameters.ECOMM_NAME))
        Assert.assertEquals(data[Parameters.ECOMM_PRODUCTS], listOf(product1, product2))
        Assert.assertNull(data[Parameters.ECOMM_CART_ID])
        Assert.assertEquals(data[Parameters.ECOMM_CART_VALUE], 123.45)
        Assert.assertEquals(data[Parameters.ECOMM_CART_CURRENCY], "GBP")

        event = RemoveFromCart(listOf(product1), 0.5, "USD", "id")
        data = event.dataPayload
        Assert.assertEquals(data[Parameters.ECOMM_CART_ID], "id")
    }
}
