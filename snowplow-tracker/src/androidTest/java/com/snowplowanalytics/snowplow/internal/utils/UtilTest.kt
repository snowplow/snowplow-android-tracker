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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.utils.Util.addToMap
import com.snowplowanalytics.core.utils.Util.base64Encode
import com.snowplowanalytics.core.utils.Util.deserializer
import com.snowplowanalytics.core.utils.Util.getDateTimeFromTimestamp
import com.snowplowanalytics.core.utils.Util.getUTF8Length
import com.snowplowanalytics.core.utils.Util.joinLongList
import com.snowplowanalytics.core.utils.Util.mapHasKeys
import com.snowplowanalytics.core.utils.Util.serialize
import com.snowplowanalytics.core.utils.Util.timestamp
import com.snowplowanalytics.core.utils.Util.uUIDString
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class UtilTest {
    @Test
    fun testGetTimestamp() {
        Assert.assertEquals(13, timestamp().length.toLong())
    }

    @Test
    fun testGetDateTimeFromTimestamp() {
        val timestamp = 1653923456266L
        Assert.assertEquals("2022-05-30T15:10:56.266Z", getDateTimeFromTimestamp(timestamp))
    }

    @Test
    fun testDateTimeProducesExpectedNumerals() {
        val timestamp = 1660643130123L
        val defaultLocale = Locale.getDefault()

        // set locale to one where different numerals are used (Egypt - arabic)
        Locale.setDefault(Locale("ar", "EG"))
        Assert.assertEquals("2022-08-16T09:45:30.123Z", getDateTimeFromTimestamp(timestamp))

        // restore original locale
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun testBase64Encode() {
        Assert.assertEquals("Zm9v", base64Encode("foo"))
    }

    @Test
    fun testGetEventId() {
        val eid = uUIDString()
        Assert.assertNotNull(eid)
        Assert.assertTrue(eid.matches(Regex("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$")))
    }

    @Test
    fun testGetUTF8Length() {
        Assert.assertEquals(19, getUTF8Length("foo€♥£\uD800\uDF48\uD83C\uDF44"))
    }

    @Test
    fun testJoinLongList() {
        val list: MutableList<Long?> = ArrayList()
        list.add(1L)
        Assert.assertEquals("1", joinLongList(list))
        list.add(2L)
        list.add(3L)
        Assert.assertEquals("1,2,3", joinLongList(list))
        list.add(null)
        Assert.assertEquals("1,2,3", joinLongList(list))
        list.add(5L)
        Assert.assertEquals("1,2,3,5", joinLongList(list))
    }

    @Test
    fun testDeserialize() {
        val testMap: MutableMap<String, String> = HashMap()
        testMap["foo"] = "bar"
        val testMapBytes = serialize(testMap)
        Assert.assertNotNull(testMapBytes)
        val testMap2 = deserializer(
            testMapBytes!!
        )
        Assert.assertNotNull(testMap2)
        Assert.assertEquals("bar", testMap2!!["foo"])
    }

    @Test
    fun testMapHasKeys() {
        val map: MutableMap<String, Any> = HashMap()
        map["key"] = "value"
        Assert.assertTrue(mapHasKeys(map, "key"))
        Assert.assertFalse(mapHasKeys(map, "key2"))
        Assert.assertFalse(mapHasKeys(map, "key", "key2"))
    }

    @Test
    fun testAddToMap() {
        val map: MutableMap<String, Any> = HashMap()
        addToMap(null, null, map)
        Assert.assertEquals(0, map.size.toLong())
        addToMap("hello", null, map)
        Assert.assertEquals(0, map.size.toLong())
        addToMap("", "", map)
        Assert.assertEquals(0, map.size.toLong())
        addToMap("hello", "world", map)
        Assert.assertEquals(1, map.size.toLong())
        Assert.assertEquals("world", map["hello"])
    }
}
