/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
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

@RunWith(AndroidJUnit4::class)
class TimingTest {
    @Test
    fun testExpectedForm() {
        var timing = Timing("some category", "some var", 123456789)
            .label("some label")
        var data: Map<String, Any?> = timing.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals("some category", data[Parameters.UT_CATEGORY])
        Assert.assertEquals(123456789, data[Parameters.UT_TIMING])
        Assert.assertEquals("some var", data[Parameters.UT_VARIABLE])
        Assert.assertEquals("some label", data[Parameters.UT_LABEL])
        timing = Timing("some category", "some var", 123456789)
        data = timing.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals("some category", data[Parameters.UT_CATEGORY])
        Assert.assertEquals(123456789, data[Parameters.UT_TIMING])
        Assert.assertEquals("some var", data[Parameters.UT_VARIABLE])
        Assert.assertFalse(data.containsKey(Parameters.UT_LABEL))
        timing = Timing("some category", "some var", 123456789)
            .label("")
        data = timing.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals("some category", data[Parameters.UT_CATEGORY])
        Assert.assertEquals(123456789, data[Parameters.UT_TIMING])
        Assert.assertEquals("some var", data[Parameters.UT_VARIABLE])
        Assert.assertFalse(data.containsKey(Parameters.UT_LABEL))
        timing = Timing("some category", "some var", 123456789)
            .label(null)
        data = timing.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals("some category", data[Parameters.UT_CATEGORY])
        Assert.assertEquals(123456789, data[Parameters.UT_TIMING])
        Assert.assertEquals("some var", data[Parameters.UT_VARIABLE])
        Assert.assertFalse(data.containsKey(Parameters.UT_LABEL))
    }

    @Test
    fun testBuilderFailures() {
        var exception = false
        try {
            Timing("", "variable", 123)
        } catch (e: Exception) {
            Assert.assertEquals("category cannot be empty", e.message)
            exception = true
        }
        Assert.assertTrue(exception)
        exception = false
        try {
            Timing("category", "", 123)
        } catch (e: Exception) {
            Assert.assertEquals("variable cannot be empty", e.message)
            exception = true
        }
        Assert.assertTrue(exception)
    }
}
