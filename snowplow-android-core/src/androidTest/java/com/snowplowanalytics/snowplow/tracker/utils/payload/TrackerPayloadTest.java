package com.snowplowanalytics.snowplow.tracker.utils.payload;

import android.test.AndroidTestCase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

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

    public HashMap<String,Object> ababMap() {
        HashMap<String,Object> map = new HashMap<String,Object>();
        map.put("a", abMap());
        return map;
    }

    public void testAddStringString() throws JSONException {
        payload.add("a", "b");
        assertEquals(abMap(), payload.getMap());
        JSONObject map = new JSONObject(payload.toString());
        assertEquals("b", map.getString("a"));
    }

    public void testAddStringObject() throws JSONException {
        payload.add("a", abMap());
        assertEquals(ababMap(), payload.getMap());
        JSONObject map = new JSONObject(payload.toString());
        JSONObject innerMap = map.getJSONObject("a");
        assertEquals("b", innerMap.getString("a"));
    }

    public void testAddMapBase64Encoded() {
        // TODO fill this in
    }

}
