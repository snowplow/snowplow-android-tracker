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
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Exception

@RunWith(AndroidJUnit4::class)
@Suppress("deprecation")
class EcommerceItemTest {
    // NB this event type has been deprecated
    
    @Test
    fun testExpectedForm() {
        var ecommerceTransactionItem = EcommerceTransactionItem("some sku", 123.456, 1)
            .orderId("orderId")
        Assert.assertEquals("ti", ecommerceTransactionItem.name)
        var data: Map<*, *> = ecommerceTransactionItem.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals("orderId", data[Parameters.TI_ITEM_ID])
        Assert.assertEquals("some sku", data[Parameters.TI_ITEM_SKU])
        Assert.assertEquals("123.456", data[Parameters.TI_ITEM_PRICE])
        Assert.assertEquals("1", data[Parameters.TI_ITEM_QUANTITY])
        Assert.assertFalse(data.containsKey(Parameters.TI_ITEM_NAME))
        Assert.assertFalse(data.containsKey(Parameters.TI_ITEM_CATEGORY))
        Assert.assertFalse(data.containsKey(Parameters.TI_ITEM_CURRENCY))
        ecommerceTransactionItem = EcommerceTransactionItem("some sku", 123.456, 1)
            .name("some name")
            .category("some category")
            .currency("EUR")
            .orderId("orderId")
        data = ecommerceTransactionItem.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals("orderId", data[Parameters.TI_ITEM_ID])
        Assert.assertEquals("some sku", data[Parameters.TI_ITEM_SKU])
        Assert.assertEquals("123.456", data[Parameters.TI_ITEM_PRICE])
        Assert.assertEquals("1", data[Parameters.TI_ITEM_QUANTITY])
        Assert.assertEquals("some name", data[Parameters.TI_ITEM_NAME])
        Assert.assertEquals("some category", data[Parameters.TI_ITEM_CATEGORY])
        Assert.assertEquals("EUR", data[Parameters.TI_ITEM_CURRENCY])
    }

    @Test
    fun testBuilderFailures() {
        var exception = false
        try {
            EcommerceTransactionItem("", 123.456, 1)
        } catch (e: Exception) {
            Assert.assertEquals("sku cannot be empty", e.message)
            exception = true
        }
        Assert.assertTrue(exception)
    }
}
