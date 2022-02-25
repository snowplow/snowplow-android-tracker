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

public class StructuredTest extends AndroidTestCase {

    public void testExpectedForm() {
        Structured structured = new Structured("some category", "some action");

        assertEquals("se", structured.getName());
        Map data = structured.getDataPayload();
        assertNotNull(data);
        assertEquals("some category", data.get(Parameters.SE_CATEGORY));
        assertEquals("some action", data.get(Parameters.SE_ACTION));
        assertFalse(data.containsKey(Parameters.SE_LABEL));
        assertFalse(data.containsKey(Parameters.SE_PROPERTY));
        assertFalse(data.containsKey(Parameters.SE_VALUE));

        structured = new Structured("some category", "some action")
                .label("some label")
                .property("some property")
                .value(123.56700);
        structured.trueTimestamp(123456789L);

        data = structured.getDataPayload();

        assertNotNull(data);
        assertEquals("some category", data.get(Parameters.SE_CATEGORY));
        assertEquals("some action", data.get(Parameters.SE_ACTION));
        assertEquals("some label", data.get(Parameters.SE_LABEL));
        assertEquals("some property", data.get(Parameters.SE_PROPERTY));
        assertEquals("123.567", data.get(Parameters.SE_VALUE));
    }

    public void testBuilderFailures() {
        boolean exception = false;
        try {
            new Structured(null, null);
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            new Structured("category", null);
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            new Structured("", "hello");
        } catch (Exception e) {
            assertEquals("category cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            new Structured("category", "");
        } catch (Exception e) {
            assertEquals("action cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);
    }
}
