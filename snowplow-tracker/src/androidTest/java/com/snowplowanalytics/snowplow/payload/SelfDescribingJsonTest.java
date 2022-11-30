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

package com.snowplowanalytics.snowplow.payload;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SelfDescribingJsonTest {

    private final String testSchema = "org.test.scheme";
    private HashMap<String, Object> testMap;
    private List<Object> testList;

    @Before
    public void setUp() {
        testMap = new HashMap<>();
        testList = new ArrayList<>();
    }

    @Test
    public void testFailures() {
        boolean exception = false;
        try {
            new SelfDescribingJson(null);
        } catch (Exception e) {
            if (e.getMessage() != null) {
                assertTrue(e.getMessage().startsWith("Parameter specified as non-null is null"));
            }
            exception = true;
        }
        assertTrue(exception);

        exception = false;
        try {
            new SelfDescribingJson("");
        } catch (Exception e) {
            assertEquals("schema cannot be empty.", e.getMessage());
            exception = true;
        }
        assertTrue(exception);
    }

    @Test
    public void testCreateWithSchemaOnly() throws JSONException {
        SelfDescribingJson json = new SelfDescribingJson(testSchema);

        // {"schema":"org.test.scheme","data":{}}
        String s = json.toString();

        JSONObject map = new JSONObject(s);
        assertEquals(testSchema, map.getString("schema"));
        JSONObject d = map.getJSONObject("data");
        assertEquals(0, d.length());
    }

    @Test
    public void testCreateWithOurEmptyMap() throws JSONException {
        SelfDescribingJson json = new SelfDescribingJson(testSchema, testMap);

        // {"schema":"org.test.scheme","data":{}}
        String s = json.toString();

        JSONObject map = new JSONObject(s);
        assertEquals(testSchema, map.getString("schema"));
        JSONObject d = map.getJSONObject("data");
        assertEquals(0, d.length());
    }

    @Test
    public void testCreateWithSimpleMap() throws JSONException {
        testMap.put("alpha", "beta");
        SelfDescribingJson json = new SelfDescribingJson(testSchema, testMap);

        // {"schema":"org.test.scheme","data":{"alpha":"beta"}}
        String s = json.toString();

        JSONObject map = new JSONObject(s);
        assertEquals(testSchema, map.getString("schema"));
        JSONObject d = map.getJSONObject("data");
        assertEquals(1, d.length());
        assertEquals("beta", d.getString("alpha"));
    }

    @Test
    public void testCreateWithEmptyList() throws JSONException {
        SelfDescribingJson json = new SelfDescribingJson(testSchema, testList);

        // {"schema":"org.test.scheme","data":[]}
        String s = json.toString();

        JSONObject map = new JSONObject(s);
        assertEquals(testSchema, map.getString("schema"));
        JSONArray d = map.getJSONArray("data");
        assertEquals(0, d.length());
    }

    @Test
    public void testCreateWithSimpleList() throws JSONException {
        testList.add("delta");
        SelfDescribingJson json = new SelfDescribingJson(testSchema, testList);

        // {"schema":"org.test.scheme","data":["delta"]}
        String s = json.toString();

        JSONObject map = new JSONObject(s);
        assertEquals(testSchema, map.getString("schema"));
        JSONArray d = map.getJSONArray("data");
        assertEquals(1, d.length());
        assertEquals("delta", d.get(0));
    }

    @Test
    public void testCreateWithNestedList() throws JSONException {
        List<String> testInnerList = new ArrayList<>();
        testInnerList.add("gamma");
        testInnerList.add("epsilon");
        testList.add(testInnerList);
        SelfDescribingJson json = new SelfDescribingJson(testSchema, testList);

        // {"schema":"org.test.scheme","data":[["gamma","epsilon"]]}
        String s = json.toString();

        JSONObject map = new JSONObject(s);
        assertEquals(testSchema, map.getString("schema"));
        JSONArray list = map.getJSONArray("data");
        assertEquals(1, list.length());
        JSONArray innerList = list.getJSONArray(0);
        assertEquals(2, innerList.length());
        assertEquals("gamma", innerList.get(0));
        assertEquals("epsilon", innerList.get(1));
    }

    @Test
    public void testCreateWithListOfMaps() throws JSONException {
        testMap.put("a", "b");
        testList.add(testMap);
        testList.add(testMap);
        SelfDescribingJson json = new SelfDescribingJson(testSchema, testList);

        // {"schema":"org.test.scheme","data":[{"a":"b"},{"a":"b"}]}
        String s = json.toString();

        JSONObject map = new JSONObject(s);
        assertEquals(testSchema, map.getString("schema"));
        JSONArray list = map.getJSONArray("data");
        assertEquals(2, list.length());
        JSONObject innerListMap1 = list.getJSONObject(0);
        assertEquals("b", innerListMap1.getString("a"));
        JSONObject innerListMap2 = list.getJSONObject(0);
        assertEquals("b", innerListMap2.getString("a"));
    }

    @Test
    public void testCreateWithSelfDescribingJson() throws JSONException {
        SelfDescribingJson json = new SelfDescribingJson(testSchema, new SelfDescribingJson(testSchema, testMap));

        // {"schema":"org.test.scheme","data":{"schema":"org.test.scheme","data":{}}}
        String s = json.toString();

        JSONObject map = new JSONObject(s);
        assertEquals(testSchema, map.getString("schema"));
        JSONObject innerMap = map.getJSONObject("data");
        assertEquals(testSchema, innerMap.getString("schema"));
        JSONObject innerData = innerMap.getJSONObject("data");
        assertEquals(0, innerData.length());
    }

    @Test
    public void testCreateWithSelfDescribingJsonWithMore() throws JSONException {
        testMap.put("a", "b");
        testMap.put("c", "d");
        SelfDescribingJson json = new SelfDescribingJson(testSchema, new SelfDescribingJson(testSchema, testMap));

        // {"schema":"org.test.scheme","data":{"schema":"org.test.scheme","data":{"a":"b","c":"d"}}}
        String s = json.toString();

        JSONObject map = new JSONObject(s);
        assertEquals(testSchema, map.getString("schema"));
        JSONObject innerMap = map.getJSONObject("data");
        assertEquals(testSchema, innerMap.getString("schema"));
        JSONObject innerData = innerMap.getJSONObject("data");
        assertEquals(2, innerData.length());
        assertEquals("b", innerData.getString("a"));
        assertEquals("d", innerData.getString("c"));
    }

    @Test
    public void testCreateThenSetSelfDescribingJson() throws JSONException {
        SelfDescribingJson json = new SelfDescribingJson(testSchema);
        json.setData(new SelfDescribingJson(testSchema, testMap));

        // {"schema":"org.test.scheme","data":{"schema":"org.test.scheme","data":{}}}
        String s = json.toString();

        JSONObject map = new JSONObject(s);
        assertEquals(testSchema, map.getString("schema"));
        JSONObject innerMap = map.getJSONObject("data");
        assertEquals(testSchema, innerMap.getString("schema"));
        JSONObject innerData = innerMap.getJSONObject("data");
        assertEquals(0, innerData.length());
    }

    @Test
    public void testCreateWithTrackerPayload() throws JSONException {
        TrackerPayload payload = new TrackerPayload();
        testMap.put("a", "b");
        payload.addMap(testMap);
        SelfDescribingJson json = new SelfDescribingJson(testSchema, payload);

        // {"schema":"org.test.scheme","data":{"a":"b"}}
        String s = json.toString();

        JSONObject map = new JSONObject(s);
        assertEquals(testSchema, map.getString("schema"));
        JSONObject innerMap = map.getJSONObject("data");
        assertEquals("b", innerMap.getString("a"));
    }

    @Test
    public void testCreateThenSetTrackerPayload() throws JSONException {
        TrackerPayload payload = new TrackerPayload();
        testMap.put("a", "b");
        payload.addMap(testMap);
        SelfDescribingJson json = new SelfDescribingJson(testSchema);
        json.setData(payload);

        // {"schema":"org.test.scheme","data":{"a":"b"}}
        String s = json.toString();

        JSONObject map = new JSONObject(s);
        assertEquals(testSchema, map.getString("schema"));
        JSONObject innerMap = map.getJSONObject("data");
        assertEquals("b", innerMap.getString("a"));
    }

    @Test
    public void testSetNullValues() {
        SelfDescribingJson json = new SelfDescribingJson(testSchema);
        assertEquals("{\"schema\":\"org.test.scheme\",\"data\":{}}", json.toString());
        json.setData((TrackerPayload) null);
        assertEquals("{\"schema\":\"org.test.scheme\",\"data\":{}}", json.toString());
        json.setData((Object) null);
        assertEquals("{\"schema\":\"org.test.scheme\",\"data\":{}}", json.toString());
        json.setData((SelfDescribingJson) null);
        assertEquals("{\"schema\":\"org.test.scheme\",\"data\":{}}", json.toString());
    }

    @Test
    public void testGetByteSize() {
        TrackerPayload payload = new TrackerPayload();
        testMap.put("a", "b");
        payload.addMap(testMap);
        SelfDescribingJson json = new SelfDescribingJson(testSchema);
        json.setData(payload);
        assertEquals(45, json.getByteSize());
    }
}
