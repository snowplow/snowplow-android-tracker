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

import androidx.test.espresso.core.internal.deps.guava.collect.Lists;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EcommerceTest extends AndroidTestCase {

    public void testExpectedForm() {
        EcommerceTransaction ecommerceTransaction = new EcommerceTransaction("some order id", 123.456, new ArrayList<EcommerceTransactionItem>());

        assertEquals("tr", ecommerceTransaction.getName());
        Map data = ecommerceTransaction.getDataPayload();

        assertNotNull(data);
        assertEquals("some order id", data.get(Parameters.TR_ID));
        assertEquals("123.456", data.get(Parameters.TR_TOTAL));
        assertFalse(data.containsKey(Parameters.TR_AFFILIATION));
        assertFalse(data.containsKey(Parameters.TR_TAX));
        assertFalse(data.containsKey(Parameters.TR_SHIPPING));
        assertFalse(data.containsKey(Parameters.TR_CITY));
        assertFalse(data.containsKey(Parameters.TR_STATE));
        assertFalse(data.containsKey(Parameters.TR_COUNTRY));
        assertFalse(data.containsKey(Parameters.TR_CURRENCY));

        EcommerceTransactionItem ecommerceTransactionItem = new EcommerceTransactionItem("some sku", 123.456, 1);
        List<EcommerceTransactionItem> items = Lists.newArrayList(ecommerceTransactionItem);

        ecommerceTransaction = new EcommerceTransaction("some order id", 123.456, items)
                .affiliation("some affiliate")
                .taxValue(50.6)
                .shipping(10.0)
                .city("Dijon")
                .state("Bourgogne")
                .country("France")
                .currency("EUR");

        data = ecommerceTransaction.getDataPayload();

        assertNotNull(data);
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
            new EcommerceTransaction(null, null, null);
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            new EcommerceTransaction("some order id", null, new ArrayList<EcommerceTransactionItem>());
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            new EcommerceTransaction("some order id", 123.456, null);
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            new EcommerceTransaction("", 123.456, new ArrayList<EcommerceTransactionItem>());
        } catch (Exception e) {
            assertEquals("orderId cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);
    }
}
