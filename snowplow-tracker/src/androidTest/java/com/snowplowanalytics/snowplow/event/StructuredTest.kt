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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.constants.Parameters
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StructuredTest {
    @Test
    fun testExpectedForm() {
        var structured = Structured("some category", "some action")
        Assert.assertEquals("se", structured.name)
        var data: Map<*, *> = structured.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals("some category", data[Parameters.SE_CATEGORY])
        Assert.assertEquals("some action", data[Parameters.SE_ACTION])
        Assert.assertFalse(data.containsKey(Parameters.SE_LABEL))
        Assert.assertFalse(data.containsKey(Parameters.SE_PROPERTY))
        Assert.assertFalse(data.containsKey(Parameters.SE_VALUE))
        structured = Structured("some category", "some action")
            .label("some label")
            .property("some property")
            .value(123.56700)
        structured.trueTimestamp(123456789L)
        data = structured.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals("some category", data[Parameters.SE_CATEGORY])
        Assert.assertEquals("some action", data[Parameters.SE_ACTION])
        Assert.assertEquals("some label", data[Parameters.SE_LABEL])
        Assert.assertEquals("some property", data[Parameters.SE_PROPERTY])
        Assert.assertEquals("123.567", data[Parameters.SE_VALUE])
    }

    @Test
    fun testBuilderFailures() {
        var exception = false
        try {
            Structured("", "hello")
        } catch (e: Exception) {
            Assert.assertEquals("category cannot be empty", e.message)
            exception = true
        }
        Assert.assertTrue(exception)
        exception = false
        try {
            Structured("category", "")
        } catch (e: Exception) {
            Assert.assertEquals("action cannot be empty", e.message)
            exception = true
        }
        Assert.assertTrue(exception)
    }
}
