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
import com.snowplowanalytics.snowplow.ecommerce.events.TransactionEvent
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class TransactionTest {
    @Test
    fun testExpectedForm() {
        val product1 = ProductEntity(
            id = "product ID",
            name = "product name",
            category = "category",
            currency = "JPY",
            price = 123456789
        )
        val product2 = ProductEntity(
            id = "id",
            price = 0.99,
            category = "category2",
            currency = "GBP"
        )

        val event = TransactionEvent("transactionId",
            8999, 
            "EUR", 
            "visa",
            2000,
            products = listOf(product1, product2)
        )

        val map = hashMapOf<String, Any>(
            "schema" to TrackerConstants.SCHEMA_ECOMMERCE_TRANSACTION,
            "data" to hashMapOf(
                Parameters.ECOMM_TRANSACTION_ID to "transactionId",
                Parameters.ECOMM_TRANSACTION_REVENUE to 8999,
                Parameters.ECOMM_TRANSACTION_CURRENCY to "EUR",
                Parameters.ECOMM_TRANSACTION_PAYMENT_METHOD to "visa",
                Parameters.ECOMM_TRANSACTION_QUANTITY to 2000,
            )
        )
        
        val data: Map<String, Any?> = event.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals(EcommerceAction.transaction.toString(), data[Parameters.ECOMM_TYPE])
        Assert.assertFalse(data.containsKey(Parameters.ECOMM_TRANSACTION_REVENUE))

        val entities = event.entitiesForProcessing
        Assert.assertNotNull(entities)
        Assert.assertEquals(3, entities!!.size)
        Assert.assertEquals(map, entities[2].map)
    }
}
