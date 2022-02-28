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

package com.snowplowanalytics.snowplow.event;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;

import java.util.Map;

public class EcommerceItemTest extends AndroidTestCase {

    public void testExpectedForm() {
        EcommerceTransactionItem ecommerceTransactionItem = new EcommerceTransactionItem("some sku", 123.456, 1)
                .orderId("orderId");

        assertEquals("ti", ecommerceTransactionItem.getName());
        Map data = ecommerceTransactionItem.getDataPayload();

        assertNotNull(data);
        assertEquals("orderId", data.get(Parameters.TI_ITEM_ID));
        assertEquals("some sku", data.get(Parameters.TI_ITEM_SKU));
        assertEquals("123.456", data.get(Parameters.TI_ITEM_PRICE));
        assertEquals("1", data.get(Parameters.TI_ITEM_QUANTITY));
        assertFalse(data.containsKey(Parameters.TI_ITEM_NAME));
        assertFalse(data.containsKey(Parameters.TI_ITEM_CATEGORY));
        assertFalse(data.containsKey(Parameters.TI_ITEM_CURRENCY));

        ecommerceTransactionItem = new EcommerceTransactionItem("some sku", 123.456, 1)
                .name("some name")
                .category("some category")
                .currency("EUR")
                .orderId("orderId");

        data = ecommerceTransactionItem.getDataPayload();

        assertNotNull(data);
        assertEquals("orderId", data.get(Parameters.TI_ITEM_ID));
        assertEquals("some sku", data.get(Parameters.TI_ITEM_SKU));
        assertEquals("123.456", data.get(Parameters.TI_ITEM_PRICE));
        assertEquals("1", data.get(Parameters.TI_ITEM_QUANTITY));
        assertEquals("some name", data.get(Parameters.TI_ITEM_NAME));
        assertEquals("some category", data.get(Parameters.TI_ITEM_CATEGORY));
        assertEquals("EUR", data.get(Parameters.TI_ITEM_CURRENCY));
    }

    public void testBuilderFailures() {
        boolean exception = false;
        try {
            new EcommerceTransactionItem("", 123.456, 1);
        } catch (Exception e) {
            assertEquals("sku cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);
    }
}
