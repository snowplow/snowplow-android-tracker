package com.snowplowanalytics.snowplow.tracker.utils.payload;

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

    public HashMap<String,Object> ababMap() {
        HashMap<String,Object> map = new HashMap<String,Object>();
        map.put("a", abMap());
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

        // {"enc":"eyJhIjoiYiJ9\n"}
        String s = payload.toString();

        JSONObject map = new JSONObject(s);
        String value = map.getString("enc");
        assertEquals("eyJhIjoiYiJ9\n", value);
        String decoded = new String(Base64.decode(value, Base64.URL_SAFE));
        assertEquals("{\"a\":\"b\"}", decoded);
    }

}
