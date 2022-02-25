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

import android.test.AndroidTestCase;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class TrackerPayloadTest extends AndroidTestCase {

    TrackerPayload payload;

    public void setUp() {
        payload = new TrackerPayload();
    }

    public HashMap<String,Object> createTestMap() {
        HashMap<String,Object> map = new HashMap<>();
        map.put("a", "string");
        map.put("b", "");
        map.put("c", null);
        return map;
    }

    public void testAddKeyWhenValue() {
        payload.add("a", "string");
        assertEquals("string", payload.getMap().get("a"));
        payload.add("a", 123);
        assertEquals(123, payload.getMap().get("a"));
    }

    public void testNotAddKeyWhenNullOrEmptyValue() {
        payload.add("a", null);
        assertFalse(payload.getMap().containsKey("a"));
        payload.add("a", "");
        assertFalse(payload.getMap().containsKey("a"));
    }

    public void testRemoveKeyWhenNullOrEmptyValue() {
        payload.add("a", "string");
        payload.add("a", "");
        assertFalse(payload.getMap().containsKey("a"));
        payload.add("a", 123);
        payload.add("a", null);
        assertFalse(payload.getMap().containsKey("a"));
    }
    
    public void testAddMapWithoutNullValueEntries() {
        Map<String,Object> testMap = new HashMap<String,Object>(createTestMap());
        payload.addMap(testMap);
        assertEquals("string", payload.getMap().get("a"));
        assertEquals("", payload.getMap().get("b"));
        assertFalse(payload.getMap().containsKey("c"));
    }

    public void testDontCrashWhenAddNullMap() {
        payload.addMap(null);
        assertTrue(payload.getMap().isEmpty());
        payload.addMap(null, true, "a", "b");
        assertTrue(payload.getMap().isEmpty());
    }

    public void testAddSimpleMapBase64NoEncode() throws JSONException {
        payload.addMap(createTestMap(), false, "enc", "no_enc");
        Map<String, Object> map = payload.getMap();
        assertFalse(map.containsKey("enc"));
        assertTrue(map.containsKey("no_enc"));
        JSONObject json = new JSONObject((String) map.get("no_enc"));
        assertEquals("string", json.getString("a"));
        assertEquals("", json.getString("b"));
        assertEquals(JSONObject.NULL, json.get("c"));
    }

    public void testAddMapBase64Encoded() throws JSONException {
        payload.addMap(createTestMap(), true, "enc", "no_enc");
        Map<String, Object> map = payload.getMap();
        assertFalse(map.containsKey("no_enc"));
        String base64Json = (String) payload.getMap().get("enc");
        String jsonString = "";
        try {
            jsonString = new String(Base64.decode(base64Json, Base64.DEFAULT), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            fail("UnsupportedEncodingException");
        }
        JSONObject json = new JSONObject(jsonString);
        assertEquals("string", json.getString("a"));
        assertEquals("", json.getString("b"));
        assertEquals(JSONObject.NULL, json.get("c"));
    }

    public void testSimplePayloadToString() throws JSONException {
        payload.add("a", "string");
        JSONObject map = new JSONObject(payload.toString());
        assertEquals("string", map.getString("a"));
    }

    public void testComplexPayloadToString() throws JSONException {
        payload.add("a", createTestMap());
        JSONObject map = new JSONObject(payload.toString());
        JSONObject innerMap = map.getJSONObject("a");
        assertEquals("string", innerMap.getString("a"));
    }
}
