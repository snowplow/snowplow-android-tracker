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

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelfDescribingJsonTest {
    private val testSchema = "org.test.scheme"
    private var testMap: HashMap<String, Any?>? = null
    private var testList: MutableList<Any?>? = null
    @Before
    fun setUp() {
        testMap = HashMap()
        testList = ArrayList()
    }

    @Test
    fun testFailures() {
        var exception = false
        try {
            SelfDescribingJson("")
        } catch (e: Exception) {
            Assert.assertEquals("schema cannot be empty", e.message)
            exception = true
        }
        Assert.assertTrue(exception)
    }

    @Test
    @Throws(JSONException::class)
    fun testCreateWithSchemaOnly() {
        val json = SelfDescribingJson(testSchema)

        // {"schema":"org.test.scheme","data":{}}
        val s = json.toString()
        val map = JSONObject(s)
        Assert.assertEquals(testSchema, map.getString("schema"))
        val d = map.getJSONObject("data")
        Assert.assertEquals(0, d.length().toLong())
    }

    @Test
    @Throws(JSONException::class)
    fun testCreateWithOurEmptyMap() {
        val json = SelfDescribingJson(testSchema, testMap!!)

        // {"schema":"org.test.scheme","data":{}}
        val s = json.toString()
        val map = JSONObject(s)
        Assert.assertEquals(testSchema, map.getString("schema"))
        val d = map.getJSONObject("data")
        Assert.assertEquals(0, d.length().toLong())
    }

    @Test
    @Throws(JSONException::class)
    fun testCreateWithSimpleMap() {
        testMap!!["alpha"] = "beta"
        val json = SelfDescribingJson(testSchema, testMap!!)

        // {"schema":"org.test.scheme","data":{"alpha":"beta"}}
        val s = json.toString()
        val map = JSONObject(s)
        Assert.assertEquals(testSchema, map.getString("schema"))
        val d = map.getJSONObject("data")
        Assert.assertEquals(1, d.length().toLong())
        Assert.assertEquals("beta", d.getString("alpha"))
    }

    @Test
    @Throws(JSONException::class)
    fun testCreateWithEmptyList() {
        val json = SelfDescribingJson(testSchema, testList!!)

        // {"schema":"org.test.scheme","data":[]}
        val s = json.toString()
        val map = JSONObject(s)
        Assert.assertEquals(testSchema, map.getString("schema"))
        val d = map.getJSONArray("data")
        Assert.assertEquals(0, d.length().toLong())
    }

    @Test
    @Throws(JSONException::class)
    fun testCreateWithSimpleList() {
        testList!!.add("delta")
        val json = SelfDescribingJson(testSchema, testList!!)

        // {"schema":"org.test.scheme","data":["delta"]}
        val s = json.toString()
        val map = JSONObject(s)
        Assert.assertEquals(testSchema, map.getString("schema"))
        val d = map.getJSONArray("data")
        Assert.assertEquals(1, d.length().toLong())
        Assert.assertEquals("delta", d[0])
    }

    @Test
    @Throws(JSONException::class)
    fun testCreateWithNestedList() {
        val testInnerList: MutableList<String> = ArrayList()
        testInnerList.add("gamma")
        testInnerList.add("epsilon")
        testList!!.add(testInnerList)
        val json = SelfDescribingJson(testSchema, testList!!)

        // {"schema":"org.test.scheme","data":[["gamma","epsilon"]]}
        val s = json.toString()
        val map = JSONObject(s)
        Assert.assertEquals(testSchema, map.getString("schema"))
        val list = map.getJSONArray("data")
        Assert.assertEquals(1, list.length().toLong())
        val innerList = list.getJSONArray(0)
        Assert.assertEquals(2, innerList.length().toLong())
        Assert.assertEquals("gamma", innerList[0])
        Assert.assertEquals("epsilon", innerList[1])
    }

    @Test
    @Throws(JSONException::class)
    fun testCreateWithListOfMaps() {
        testMap!!["a"] = "b"
        testList!!.add(testMap)
        testList!!.add(testMap)
        val json = SelfDescribingJson(testSchema, testList!!)

        // {"schema":"org.test.scheme","data":[{"a":"b"},{"a":"b"}]}
        val s = json.toString()
        val map = JSONObject(s)
        Assert.assertEquals(testSchema, map.getString("schema"))
        val list = map.getJSONArray("data")
        Assert.assertEquals(2, list.length().toLong())
        val innerListMap1 = list.getJSONObject(0)
        Assert.assertEquals("b", innerListMap1.getString("a"))
        val innerListMap2 = list.getJSONObject(0)
        Assert.assertEquals("b", innerListMap2.getString("a"))
    }

    @Test
    @Throws(JSONException::class)
    fun testCreateWithSelfDescribingJson() {
        val json = SelfDescribingJson(testSchema, SelfDescribingJson(testSchema, testMap!!))

        // {"schema":"org.test.scheme","data":{"schema":"org.test.scheme","data":{}}}
        val s = json.toString()
        val map = JSONObject(s)
        Assert.assertEquals(testSchema, map.getString("schema"))
        val innerMap = map.getJSONObject("data")
        Assert.assertEquals(testSchema, innerMap.getString("schema"))
        val innerData = innerMap.getJSONObject("data")
        Assert.assertEquals(0, innerData.length().toLong())
    }

    @Test
    @Throws(JSONException::class)
    fun testCreateWithSelfDescribingJsonWithMore() {
        testMap!!["a"] = "b"
        testMap!!["c"] = "d"
        val json = SelfDescribingJson(testSchema, SelfDescribingJson(testSchema, testMap!!))

        // {"schema":"org.test.scheme","data":{"schema":"org.test.scheme","data":{"a":"b","c":"d"}}}
        val s = json.toString()
        val map = JSONObject(s)
        Assert.assertEquals(testSchema, map.getString("schema"))
        val innerMap = map.getJSONObject("data")
        Assert.assertEquals(testSchema, innerMap.getString("schema"))
        val innerData = innerMap.getJSONObject("data")
        Assert.assertEquals(2, innerData.length().toLong())
        Assert.assertEquals("b", innerData.getString("a"))
        Assert.assertEquals("d", innerData.getString("c"))
    }

    @Test
    @Throws(JSONException::class)
    fun testCreateThenSetSelfDescribingJson() {
        val json = SelfDescribingJson(testSchema)
        json.setData(SelfDescribingJson(testSchema, testMap!!))

        // {"schema":"org.test.scheme","data":{"schema":"org.test.scheme","data":{}}}
        val s = json.toString()
        val map = JSONObject(s)
        Assert.assertEquals(testSchema, map.getString("schema"))
        val innerMap = map.getJSONObject("data")
        Assert.assertEquals(testSchema, innerMap.getString("schema"))
        val innerData = innerMap.getJSONObject("data")
        Assert.assertEquals(0, innerData.length().toLong())
    }

    @Test
    @Throws(JSONException::class)
    fun testCreateWithTrackerPayload() {
        val payload = TrackerPayload()
        testMap!!["a"] = "b"
        payload.addMap(testMap!!)
        val json = SelfDescribingJson(testSchema, payload)

        // {"schema":"org.test.scheme","data":{"a":"b"}}
        val s = json.toString()
        val map = JSONObject(s)
        Assert.assertEquals(testSchema, map.getString("schema"))
        val innerMap = map.getJSONObject("data")
        Assert.assertEquals("b", innerMap.getString("a"))
    }

    @Test
    @Throws(JSONException::class)
    fun testCreateThenSetTrackerPayload() {
        val payload = TrackerPayload()
        testMap!!["a"] = "b"
        payload.addMap(testMap!!)
        val json = SelfDescribingJson(testSchema)
        json.setData(payload)

        // {"schema":"org.test.scheme","data":{"a":"b"}}
        val s = json.toString()
        val map = JSONObject(s)
        Assert.assertEquals(testSchema, map.getString("schema"))
        val innerMap = map.getJSONObject("data")
        Assert.assertEquals("b", innerMap.getString("a"))
    }

    @Test
    fun testSetNullValues() {
        val json = SelfDescribingJson(testSchema)
        Assert.assertEquals("{\"schema\":\"org.test.scheme\",\"data\":{}}", json.toString())
        json.setData(null as TrackerPayload?)
        Assert.assertEquals("{\"schema\":\"org.test.scheme\",\"data\":{}}", json.toString())
        json.setData(null as Any?)
        Assert.assertEquals("{\"schema\":\"org.test.scheme\",\"data\":{}}", json.toString())
        json.setData(null as SelfDescribingJson?)
        Assert.assertEquals("{\"schema\":\"org.test.scheme\",\"data\":{}}", json.toString())
    }

    @Test
    fun testGetByteSize() {
        val payload = TrackerPayload()
        testMap!!["a"] = "b"
        payload.addMap(testMap!!)
        val json = SelfDescribingJson(testSchema)
        json.setData(payload)
        Assert.assertEquals(45, json.byteSize)
    }
}
