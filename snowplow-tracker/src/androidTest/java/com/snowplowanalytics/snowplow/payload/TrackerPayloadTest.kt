/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.payload

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.utils.Util.objectMapToString
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.UnsupportedEncodingException

@RunWith(AndroidJUnit4::class)
class TrackerPayloadTest {
    var payload: TrackerPayload? = null
    @Before
    fun setUp() {
        payload = TrackerPayload()
    }

    private fun createTestMap(): HashMap<String, Any?> {
        val map = HashMap<String, Any?>()
        map["a"] = "string"
        map["b"] = ""
        map["c"] = null
        return map
    }

    @Test
    fun testAddKeyWhenValue() {
        payload!!.add("a", "string")
        Assert.assertEquals("string", payload!!.map["a"])
        payload!!.add("a", 123)
        Assert.assertEquals(123, payload!!.map["a"])
    }

    @Test
    fun testNotAddKeyWhenNullOrEmptyValue() {
        payload!!.add("a", null)
        Assert.assertFalse(payload!!.map.containsKey("a"))
        payload!!.add("a", "")
        Assert.assertFalse(payload!!.map.containsKey("a"))
    }

    @Test
    fun testRemoveKeyWhenNullOrEmptyValue() {
        payload!!.add("a", "string")
        payload!!.add("a", "")
        Assert.assertFalse(payload!!.map.containsKey("a"))
        payload!!.add("a", 123)
        payload!!.add("a", null)
        Assert.assertFalse(payload!!.map.containsKey("a"))
    }

    @Test
    fun testAddMapWithoutNullValueEntries() {
        val testMap: Map<String, Any?> = HashMap(createTestMap())
        payload!!.addMap(testMap)
        Assert.assertEquals("string", payload!!.map["a"])
        Assert.assertEquals("", payload!!.map["b"])
        Assert.assertFalse(payload!!.map.containsKey("c"))
    }

    @Test
    @Throws(JSONException::class)
    fun testAddSimpleMapBase64NoEncode() {
        payload!!.addMap(createTestMap(), false, "enc", "no_enc")
        val map: Map<String, Any> = payload!!.map
        Assert.assertFalse(map.containsKey("enc"))
        Assert.assertTrue(map.containsKey("no_enc"))
        val json = JSONObject(map["no_enc"] as String)
        Assert.assertEquals("string", json.getString("a"))
        Assert.assertEquals("", json.getString("b"))
        Assert.assertEquals(JSONObject.NULL, json["c"])
    }

    @Test
    @Throws(JSONException::class)
    fun testAddMapBase64Encoded() {
        payload!!.addMap(createTestMap(), true, "enc", "no_enc")
        val map = objectMapToString(
            payload!!.map
        )
        Assert.assertFalse(map.containsKey("no_enc"))
        val base64Json = payload!!.map["enc"] as String?
        var jsonString = ""
        try {
            jsonString = String(Base64.decode(base64Json, Base64.DEFAULT), Charsets.UTF_8)
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            Assert.fail("UnsupportedEncodingException")
        }
        val json = JSONObject(jsonString)
        Assert.assertEquals("string", json.getString("a"))
        Assert.assertEquals("", json.getString("b"))
        Assert.assertEquals(JSONObject.NULL, json["c"])
    }

    @Test
    @Throws(JSONException::class)
    fun testSimplePayloadToString() {
        payload!!.add("a", "string")
        val map = JSONObject(payload.toString())
        Assert.assertEquals("string", map.getString("a"))
    }

    @Test
    @Throws(JSONException::class)
    fun testComplexPayloadToString() {
        payload!!.add("a", createTestMap())
        val map = JSONObject(payload.toString())
        val innerMap = map.getJSONObject("a")
        Assert.assertEquals("string", innerMap.getString("a"))
    }
}
