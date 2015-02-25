package com.snowplowanalytics.snowplow.tracker.utils.payload;

import android.test.AndroidTestCase;

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

    public void testAddStringString() {
        payload.add("a", "b");
        assertEquals(abMap(), payload.getMap());
        assertEquals("{\"a\":\"b\"}", payload.toString());
    }

    public void testAddStringObject() {
        payload.add("a", abMap());
        assertEquals(ababMap(), payload.getMap());
        assertEquals("{\"a\":{\"a\":\"b\"}}", payload.toString());
    }

    public void testAddMapBase64Encoded() {
        // TODO fill this in
    }

}
