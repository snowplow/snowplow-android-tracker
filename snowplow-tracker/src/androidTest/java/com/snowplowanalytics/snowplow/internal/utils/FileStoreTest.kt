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
package com.snowplowanalytics.snowplow.internal.utils

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.tracker.Logger.updateLogLevel
import com.snowplowanalytics.core.session.FileStore.deleteFile
import com.snowplowanalytics.core.session.FileStore.saveMapToFile
import com.snowplowanalytics.core.session.FileStore.getMapFromFile
import com.snowplowanalytics.snowplow.tracker.LogLevel
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.HashMap

@RunWith(AndroidJUnit4::class)
class FileStoreTest {
    private val fileName = "test"
    @Before
    fun setup() {
        updateLogLevel(LogLevel.DEBUG)
    }

    @After
    fun cleanup() {
        deleteFile(fileName, InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @Test
    fun testSaveMapToFile() {
        setup()
        val map: MutableMap<String?, String?> = HashMap()
        map["hello"] = "world"
        val result = saveMapToFile(fileName, map, InstrumentationRegistry.getInstrumentation().targetContext)
        Assert.assertTrue(result)
        cleanup()
    }

    @Test
    fun testGetMapFromFile() {
        setup()
        val map: MutableMap<String?, String?> = HashMap()
        map["hello"] = "world"
        val result = saveMapToFile(fileName, map, InstrumentationRegistry.getInstrumentation().targetContext)
        Assert.assertTrue(result)
        val mapRes: Map<*, *>? = getMapFromFile(fileName, InstrumentationRegistry.getInstrumentation().targetContext)
        Assert.assertNotNull(mapRes)
        Assert.assertTrue(mapRes!!.containsKey("hello"))
        Assert.assertEquals("world", mapRes["hello"])
        cleanup()
    }

    @Test
    fun testDeleteFile() {
        setup()
        val map: MutableMap<String?, String?> = HashMap()
        map["hello"] = "world"
        val result = saveMapToFile(fileName, map, InstrumentationRegistry.getInstrumentation().targetContext)
        Assert.assertTrue(result)
        val delRes = deleteFile(fileName, InstrumentationRegistry.getInstrumentation().targetContext)
        Assert.assertTrue(delRes)
    }
}
