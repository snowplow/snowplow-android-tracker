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

public class TimingTest extends AndroidTestCase {

    public void testExpectedForm() {
        Timing timing = Timing.builder()
                .category("some category")
                .timing(123456789)
                .variable("some var")
                .label("some label")
                .build();

        Map<String, Object> data = timing.getData();

        assertNotNull(data);
        assertEquals("some category", data.get(Parameters.UT_CATEGORY));
        assertEquals(123456789, data.get(Parameters.UT_TIMING));
        assertEquals("some var", data.get(Parameters.UT_VARIABLE));
        assertEquals("some label", data.get(Parameters.UT_LABEL));

        timing = Timing.builder()
                .category("some category")
                .timing(123456789)
                .variable("some var")
                .build();

        data = timing.getData();

        assertNotNull(data);
        assertEquals("some category", data.get(Parameters.UT_CATEGORY));
        assertEquals(123456789, data.get(Parameters.UT_TIMING));
        assertEquals("some var", data.get(Parameters.UT_VARIABLE));
        assertFalse(data.containsKey(Parameters.UT_LABEL));

        timing = Timing.builder()
                .category("some category")
                .timing(123456789)
                .variable("some var")
                .label("")
                .build();

        data = timing.getData();

        assertNotNull(data);
        assertEquals("some category", data.get(Parameters.UT_CATEGORY));
        assertEquals(123456789, data.get(Parameters.UT_TIMING));
        assertEquals("some var", data.get(Parameters.UT_VARIABLE));
        assertFalse(data.containsKey(Parameters.UT_LABEL));

        timing = Timing.builder()
                .category("some category")
                .timing(123456789)
                .variable("some var")
                .label(null)
                .build();

        data = timing.getData();

        assertNotNull(data);
        assertEquals("some category", data.get(Parameters.UT_CATEGORY));
        assertEquals(123456789, data.get(Parameters.UT_TIMING));
        assertEquals("some var", data.get(Parameters.UT_VARIABLE));
        assertFalse(data.containsKey(Parameters.UT_LABEL));
    }

    public void testBuilderFailures() {
        boolean exception = false;
        try {
            Timing.builder().build();
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            Timing.builder().category("category").build();
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            Timing.builder().category("category").timing(123).build();
        } catch (Exception e) {
            assertEquals(null, e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            Timing.builder().category("").timing(123).variable("variable").build();
        } catch (Exception e) {
            assertEquals("category cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            Timing.builder().category("category").timing(123).variable("").build();
        } catch (Exception e) {
            assertEquals("variable cannot be empty", e.getMessage());
            exception = true;
        }
        assertTrue(exception);
    }
}
