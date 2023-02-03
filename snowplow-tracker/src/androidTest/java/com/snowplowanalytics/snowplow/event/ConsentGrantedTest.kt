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
import java.util.*

@RunWith(AndroidJUnit4::class)
class ConsentGrantedTest {
    @Test
    fun testExpectedForm() {
        var event = ConsentGranted("expiration", "id", "v1.0")
            .documentName("name")
            .documentDescription("description")
        var data: Map<String, Any?> = event.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals(
            "expiration",
            data[Parameters.CG_EXPIRY]
        )
        val documents: MutableList<ConsentDocument> = LinkedList()
        documents.add(
            ConsentDocument("granted context id 1", "granted context version 1")
                .documentDescription("granted context desc 1")
                .documentName("granted context name 1")
        )
        documents.add(
            ConsentDocument("granted context id 2", "granted context version 2")
                .documentDescription("granted context desc 2")
                .documentName("granted context name 2")
        )
        event = ConsentGranted("expiration", "id", "v1.0")
            .documentName("name")
            .documentDescription("description")
            .documents(documents)
        data = event.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals(
            "expiration",
            data[Parameters.CG_EXPIRY]
        )
    }

    @Test
    fun testBuilderFailures() {
        var exception = false
        try {
            ConsentGranted("", "", "test")
        } catch (e: Exception) {
            Assert.assertEquals("Expiry cannot be empty", e.message)
            exception = true
        }
        Assert.assertTrue(exception)
        exception = false
        try {
            ConsentGranted("test", "", "test")
        } catch (e: Exception) {
            Assert.assertEquals("Document ID cannot be empty", e.message)
            exception = true
        }
        Assert.assertTrue(exception)
        exception = false
        try {
            ConsentGranted("test", "test", "")
        } catch (e: Exception) {
            Assert.assertEquals("Document version cannot be empty", e.message)
            exception = true
        }
        Assert.assertTrue(exception)
    }
}
