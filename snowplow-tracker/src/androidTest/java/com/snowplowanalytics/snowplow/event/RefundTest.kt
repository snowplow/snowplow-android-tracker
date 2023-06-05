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
import com.snowplowanalytics.snowplow.ecommerce.entities.Product
import com.snowplowanalytics.snowplow.ecommerce.entities.RefundDetails
import com.snowplowanalytics.snowplow.ecommerce.entities.TransactionDetails
import com.snowplowanalytics.snowplow.ecommerce.events.ProductListView
import com.snowplowanalytics.snowplow.ecommerce.events.Refund
import com.snowplowanalytics.snowplow.ecommerce.events.Transaction
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class RefundTest {
    @Test
    fun testExpectedForm() {
        val refund = RefundDetails("id", "USD", 123.45)

        val product1 = Product(
            id = "product ID",
            name = "product name",
            category = "category",
            currency = "JPY",
            price = 123456789
        )

        val event = Refund(refund, listOf(product1))
        val data: Map<String, Any?> = event.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals(data[Parameters.ECOMM_TYPE], EcommerceAction.refund)
        Assert.assertTrue(data.containsKey(Parameters.ECOMM_PRODUCTS))
        Assert.assertEquals(data[Parameters.ECOMM_PRODUCTS], listOf(product1))
    }
}
