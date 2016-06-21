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

package com.snowplowanalytics.snowplow.tracker.payload;

import android.test.AndroidTestCase;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class TrackerPayloadTest extends AndroidTestCase {

    TrackerPayload payload;

    public void setUp() {
        payload = new TrackerPayload();
    }

    public HashMap<String,String> abMap() {
        HashMap<String,String> map = new HashMap<String,String>();
        map.put("a", "b");
        return map;
    }

    public void testAddStringString() throws JSONException {
        payload.add("a", "b");
        JSONObject map = new JSONObject(payload.toString());
        assertEquals("b", map.getString("a"));
    }

    public void testAddStringObject() throws JSONException {
        payload.add("a", abMap());
        JSONObject map = new JSONObject(payload.toString());
        JSONObject innerMap = map.getJSONObject("a");
        assertEquals("b", innerMap.getString("a"));
    }
    
    public void testAddMap() throws JSONException {
        Map<String,Object> testMap = new HashMap<String,Object>(abMap());
        payload.addMap(testMap);
        
        // {"a":"b"}
        String s = payload.toString();
        
        JSONObject map = new JSONObject(s);
        assertEquals("b", map.getString("a"));
    }
    
    public void testAddSimpleMapBase64NoEncode() throws JSONException {
        payload.addMap(abMap(), false, "enc", "no_enc");

        // {"no_enc":"{\"a\":\"b\"}"}
        String s = payload.toString();

        JSONObject map = new JSONObject(s);
        assertEquals("{\"a\":\"b\"}", map.getString("no_enc"));
    }

    public void testAddMapBase64Encoded() throws JSONException {
        payload.addMap(abMap(), true, "enc", "no_enc");

        // {"enc":"eyJhIjoiYiJ9"}
        String s = payload.toString();

        JSONObject map = new JSONObject(s);
        String value = map.getString("enc");
        assertEquals("eyJhIjoiYiJ9", value);
        String decoded = new String(Base64.decode(value, Base64.URL_SAFE));
        assertEquals("{\"a\":\"b\"}", decoded);
    }

}
