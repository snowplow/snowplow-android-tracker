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
import com.snowplowanalytics.snowplow.ecommerce.entities.ProductEntity
import com.snowplowanalytics.snowplow.ecommerce.events.ProductViewEvent
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class ProductViewTest {
    @Test
    fun testExpectedForm() {
        val product = ProductEntity(
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
        val event = ProductViewEvent(product)

        val map = hashMapOf<String, Any>(
            "schema" to TrackerConstants.SCHEMA_ECOMMERCE_PRODUCT,
            "data" to hashMapOf<String, Any>(
                Parameters.ECOMM_PRODUCT_ID to "product ID",
                Parameters.ECOMM_PRODUCT_NAME to "product name",
                Parameters.ECOMM_PRODUCT_CATEGORY to "category",
                Parameters.ECOMM_PRODUCT_PRICE to 100,
                Parameters.ECOMM_PRODUCT_LIST_PRICE to 110,
                Parameters.ECOMM_PRODUCT_QUANTITY to 2,
                Parameters.ECOMM_PRODUCT_SIZE to "small",
                Parameters.ECOMM_PRODUCT_VARIANT to "black/black",
                Parameters.ECOMM_PRODUCT_BRAND to "Snowplow",
                Parameters.ECOMM_PRODUCT_INVENTORY_STATUS to "backorder",
                Parameters.ECOMM_PRODUCT_POSITION to 1,
                Parameters.ECOMM_PRODUCT_CURRENCY to "GBP",
                Parameters.ECOMM_PRODUCT_CREATIVE_ID to "ecomm1"
            )
        )
        
        val data: Map<String, Any?> = event.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals(EcommerceAction.product_view.toString(), data[Parameters.ECOMM_TYPE])
        Assert.assertFalse(data.containsKey(Parameters.ECOMM_PRODUCT_ID))
        Assert.assertFalse(data.containsKey(Parameters.ECOMM_NAME))

        val entities = event.entitiesForProcessing
        Assert.assertNotNull(entities)
        Assert.assertEquals(1, entities!!.size)
        Assert.assertEquals(map, entities[0].map)
    }
}
