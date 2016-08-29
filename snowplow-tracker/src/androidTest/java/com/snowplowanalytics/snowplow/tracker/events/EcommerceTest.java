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

import java.util.ArrayList;
import java.util.Map;

public class EcommerceTest extends AndroidTestCase {

    public void testExpectedForm() {
        EcommerceTransaction ecommerceTransaction = EcommerceTransaction.builder()
                .orderId("some order id")
                .totalValue(123.456)
                .items(new ArrayList<EcommerceTransactionItem>())
                .build();

        Map data = ecommerceTransaction.getPayload().getMap();

        assertNotNull(data);
        assertEquals("tr", data.get(Parameters.EVENT));
        assertEquals("some order id", data.get(Parameters.TR_ID));
        assertEquals("123.456", data.get(Parameters.TR_TOTAL));
        assertFalse(data.containsKey(Parameters.TR_AFFILIATION));
        assertFalse(data.containsKey(Parameters.TR_TAX));
        assertFalse(data.containsKey(Parameters.TR_SHIPPING));
        assertFalse(data.containsKey(Parameters.TR_CITY));
        assertFalse(data.containsKey(Parameters.TR_STATE));
        assertFalse(data.containsKey(Parameters.TR_COUNTRY));
        assertFalse(data.containsKey(Parameters.TR_CURRENCY));

        EcommerceTransactionItem ecommerceTransactionItem = EcommerceTransactionItem.builder()
                .itemId("some item id")
                .sku("some sku")
                .price(123.456)
                .quantity(1)
                .build();

        ecommerceTransaction = EcommerceTransaction.builder()
                .orderId("some order id")
                .totalValue(123.456)
                .affiliation("some affiliate")
                .taxValue(50.6)
                .shipping(10.0)
                .city("Dijon")
                .state("Bourgogne")
                .country("France")
                .currency("EUR")
                .items(ecommerceTransactionItem)
                .build();

        data = ecommerceTransaction.getPayload().getMap();

        assertNotNull(data);
        assertEquals("tr", data.get(Parameters.EVENT));
        assertEquals("some order id", data.get(Parameters.TR_ID));
        assertEquals("123.456", data.get(Parameters.TR_TOTAL));
        assertEquals("some affiliate", data.get(Parameters.TR_AFFILIATION));
        assertEquals("50.6", data.get(Parameters.TR_TAX));
        assertEquals("10.0", data.get(Parameters.TR_SHIPPING));
        assertEquals("Dijon", data.get(Parameters.TR_CITY));
        assertEquals("Bourgogne", data.get(Parameters.TR_STATE));
        assertEquals("France", data.get(Parameters.TR_COUNTRY));
        assertEquals("EUR", data.get(Parameters.TR_CURRENCY));
    }

    public void testBuilderFailures() {
        boolean exception = false;
        try {
            EcommerceTransaction.builder().build();
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            EcommerceTransaction.builder().orderId("some order id").build();
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            EcommerceTransaction.builder().orderId("some order id").totalValue(123.456).build();
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            EcommerceTransaction.builder().orderId("").totalValue(123.456)
                    .items(new ArrayList<EcommerceTransactionItem>()).build();
        } catch (Exception e) {
            assertEquals("orderId cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);
    }
}
