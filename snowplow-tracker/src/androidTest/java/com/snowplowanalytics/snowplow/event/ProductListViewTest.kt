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
package com.snowplowanalytics.snowplow.event

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.ecommerce.EcommerceAction
import com.snowplowanalytics.snowplow.ecommerce.entities.ProductEntity
import com.snowplowanalytics.snowplow.ecommerce.events.ProductListViewEvent
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class ProductListViewTest {
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
            category = "category2",
            currency = "GBP"
        )
        
        var event = ProductListViewEvent(listOf(product1, product2))
        var data: Map<String, Any?> = event.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals(EcommerceAction.list_view.toString(), data[Parameters.ECOMM_TYPE])
        Assert.assertFalse(data.containsKey(Parameters.ECOMM_NAME))

        event = ProductListViewEvent(listOf(product1), "list name")
        data = event.dataPayload
        Assert.assertTrue(data.containsKey(Parameters.ECOMM_NAME))
        Assert.assertEquals(data[Parameters.ECOMM_NAME], "list name")
    }
}
