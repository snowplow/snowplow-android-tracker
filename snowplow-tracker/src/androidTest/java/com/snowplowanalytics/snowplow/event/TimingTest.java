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

public class TimingTest extends AndroidTestCase {

    public void testExpectedForm() {
        Timing timing = new Timing("some category", "some var", 123456789)
                .label("some label");

        Map<String, Object> data = timing.getDataPayload();

        assertNotNull(data);
        assertEquals("some category", data.get(Parameters.UT_CATEGORY));
        assertEquals(123456789, data.get(Parameters.UT_TIMING));
        assertEquals("some var", data.get(Parameters.UT_VARIABLE));
        assertEquals("some label", data.get(Parameters.UT_LABEL));

        timing = new Timing("some category", "some var", 123456789);

        data = timing.getDataPayload();

        assertNotNull(data);
        assertEquals("some category", data.get(Parameters.UT_CATEGORY));
        assertEquals(123456789, data.get(Parameters.UT_TIMING));
        assertEquals("some var", data.get(Parameters.UT_VARIABLE));
        assertFalse(data.containsKey(Parameters.UT_LABEL));

        timing = new Timing("some category", "some var", 123456789)
                .label("");

        data = timing.getDataPayload();

        assertNotNull(data);
        assertEquals("some category", data.get(Parameters.UT_CATEGORY));
        assertEquals(123456789, data.get(Parameters.UT_TIMING));
        assertEquals("some var", data.get(Parameters.UT_VARIABLE));
        assertFalse(data.containsKey(Parameters.UT_LABEL));

        timing = new Timing("some category", "some var", 123456789)
                .label(null);

        data = timing.getDataPayload();

        assertNotNull(data);
        assertEquals("some category", data.get(Parameters.UT_CATEGORY));
        assertEquals(123456789, data.get(Parameters.UT_TIMING));
        assertEquals("some var", data.get(Parameters.UT_VARIABLE));
        assertFalse(data.containsKey(Parameters.UT_LABEL));
    }

    public void testBuilderFailures() {
        boolean exception = false;
        try {
            new Timing(null, null, null);
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            new Timing("category", null, null);
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            new Timing("category", null, 123);
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            new Timing("", "variable", 123);
        } catch (Exception e) {
            assertEquals("category cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            new Timing("category", "", 123);
        } catch (Exception e) {
            assertEquals("variable cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);
    }
}
