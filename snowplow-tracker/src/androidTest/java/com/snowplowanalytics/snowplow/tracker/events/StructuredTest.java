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

public class StructuredTest extends AndroidTestCase {

    public void testExpectedForm() {
        Structured structured = Structured.builder()
                .category("some category")
                .action("some action")
                .build();

        Map data = structured.getPayload().getMap();

        assertNotNull(data);
        assertEquals("se", data.get(Parameters.EVENT));
        assertEquals("some category", data.get(Parameters.SE_CATEGORY));
        assertEquals("some action", data.get(Parameters.SE_ACTION));
        assertFalse(data.containsKey(Parameters.SE_LABEL));
        assertFalse(data.containsKey(Parameters.SE_PROPERTY));
        assertFalse(data.containsKey(Parameters.SE_VALUE));

        structured = Structured.builder()
                .category("some category")
                .action("some action")
                .label("some label")
                .property("some property")
                .value(123.56700)
                .build();

        data = structured.getPayload().getMap();

        assertNotNull(data);
        assertEquals("se", data.get(Parameters.EVENT));
        assertEquals("some category", data.get(Parameters.SE_CATEGORY));
        assertEquals("some action", data.get(Parameters.SE_ACTION));
        assertEquals("some label", data.get(Parameters.SE_LABEL));
        assertEquals("some property", data.get(Parameters.SE_PROPERTY));
        assertEquals("123.567", data.get(Parameters.SE_VALUE));
    }

    public void testBuilderFailures() {
        boolean exception = false;
        try {
            Structured.builder().build();
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            Structured.builder().category("category").build();
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            Structured.builder().category("").action("hello").build();
        } catch (Exception e) {
            assertEquals("category cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            Structured.builder().category("category").action("").build();
        } catch (Exception e) {
            assertEquals("action cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            Structured.builder().category("category").action("action").eventId("").build();
        } catch (Exception e) {
            assertEquals("eventId cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);
    }
}
