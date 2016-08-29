/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker.events;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;

import java.util.Map;

public class EcommerceItemTest extends AndroidTestCase {

    public void testExpectedForm() {
        EcommerceTransactionItem ecommerceTransactionItem = EcommerceTransactionItem.builder()
                .itemId("some item id")
                .sku("some sku")
                .price(123.456)
                .quantity(1)
                .build();

        Map data = ecommerceTransactionItem.getPayload().getMap();

        assertNotNull(data);
        assertEquals("ti", data.get(Parameters.EVENT));
        assertEquals("some item id", data.get(Parameters.TI_ITEM_ID));
        assertEquals("some sku", data.get(Parameters.TI_ITEM_SKU));
        assertEquals("123.456", data.get(Parameters.TI_ITEM_PRICE));
        assertEquals("1", data.get(Parameters.TI_ITEM_QUANTITY));
        assertFalse(data.containsKey(Parameters.TI_ITEM_NAME));
        assertFalse(data.containsKey(Parameters.TI_ITEM_CATEGORY));
        assertFalse(data.containsKey(Parameters.TI_ITEM_CURRENCY));

        ecommerceTransactionItem = EcommerceTransactionItem.builder()
                .itemId("some item id")
                .sku("some sku")
                .price(123.456)
                .quantity(1)
                .name("some name")
                .category("some category")
                .currency("EUR")
                .build();

        data = ecommerceTransactionItem.getPayload().getMap();

        assertNotNull(data);
        assertEquals("ti", data.get(Parameters.EVENT));
        assertEquals("some item id", data.get(Parameters.TI_ITEM_ID));
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
            EcommerceTransactionItem.builder().build();
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            EcommerceTransactionItem.builder().itemId("some item id").build();
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            EcommerceTransactionItem.builder().itemId("some item id").sku("some sku").build();
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            EcommerceTransactionItem.builder().itemId("some item id").sku("some sku").price(123.456)
                    .build();
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            EcommerceTransactionItem.builder().itemId("").sku("some sku").price(123.456)
                    .quantity(1).build();
        } catch (Exception e) {
            assertEquals("itemId cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            EcommerceTransactionItem.builder().itemId("item id").sku("").price(123.456)
                    .quantity(1).build();
        } catch (Exception e) {
            assertEquals("sku cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);
    }
}
