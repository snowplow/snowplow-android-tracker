/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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

import androidx.test.espresso.core.internal.deps.guava.collect.Lists
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.constants.Parameters
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EcommerceTest {
    
    @Test
    fun testExpectedForm() {
        var ecommerceTransaction = EcommerceTransaction("some order id", 123.456, ArrayList())
        Assert.assertEquals("tr", ecommerceTransaction.name)
        var data: Map<*, *> = ecommerceTransaction.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals("some order id", data[Parameters.TR_ID])
        Assert.assertEquals("123.456", data[Parameters.TR_TOTAL])
        Assert.assertFalse(data.containsKey(Parameters.TR_AFFILIATION))
        Assert.assertFalse(data.containsKey(Parameters.TR_TAX))
        Assert.assertFalse(data.containsKey(Parameters.TR_SHIPPING))
        Assert.assertFalse(data.containsKey(Parameters.TR_CITY))
        Assert.assertFalse(data.containsKey(Parameters.TR_STATE))
        Assert.assertFalse(data.containsKey(Parameters.TR_COUNTRY))
        Assert.assertFalse(data.containsKey(Parameters.TR_CURRENCY))
        val ecommerceTransactionItem = EcommerceTransactionItem("some sku", 123.456, 1)
        val items: List<EcommerceTransactionItem> = Lists.newArrayList(ecommerceTransactionItem)
        ecommerceTransaction = EcommerceTransaction("some order id", 123.456, items)
            .affiliation("some affiliate")
            .taxValue(50.6)
            .shipping(10.0)
            .city("Dijon")
            .state("Bourgogne")
            .country("France")
            .currency("EUR")
        data = ecommerceTransaction.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals("some order id", data[Parameters.TR_ID])
        Assert.assertEquals("123.456", data[Parameters.TR_TOTAL])
        Assert.assertEquals("some affiliate", data[Parameters.TR_AFFILIATION])
        Assert.assertEquals("50.6", data[Parameters.TR_TAX])
        Assert.assertEquals("10.0", data[Parameters.TR_SHIPPING])
        Assert.assertEquals("Dijon", data[Parameters.TR_CITY])
        Assert.assertEquals("Bourgogne", data[Parameters.TR_STATE])
        Assert.assertEquals("France", data[Parameters.TR_COUNTRY])
        Assert.assertEquals("EUR", data[Parameters.TR_CURRENCY])
    }

    @Test
    fun testBuilderFailures() {
        var exception = false
        try {
            EcommerceTransaction("", 123.456, ArrayList())
        } catch (e: Exception) {
            Assert.assertEquals("orderId cannot be empty", e.message)
            exception = true
        }
        Assert.assertTrue(exception)
    }
}
