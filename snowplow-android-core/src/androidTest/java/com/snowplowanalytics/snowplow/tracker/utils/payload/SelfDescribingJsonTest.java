package com.snowplowanalytics.snowplow.tracker.utils.payload;

import android.test.AndroidTestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SelfDescribingJsonTest extends AndroidTestCase {

    private final String testSchema = "org.test.scheme";
    private HashMap<String, Object> testMap;
    private List<Object> testList;

    public void setUp() {
        testMap = new HashMap<String,Object>();
        testList = new ArrayList<Object>();
    }

    public void testCreateWithSchemaOnly() throws JSONException {
        SelfDescribingJson json = new SelfDescribingJson(testSchema);

        // {"schema":"org.test.scheme","data":{}}
        String s = json.toString();

        JSONObject map = new JSONObject(s);
        assertEquals(testSchema, map.getString("schema"));
        JSONObject d = map.getJSONObject("data");
        assertEquals(0, d.length());

    }

    public void testCreateWithNullSchema() {
        // TODO fill in
    }

    public void testCreateWithEmptySchema() {
        // TODO fill in
    }

    public void testCreateWithOurEmptyMap() throws JSONException {
        SelfDescribingJson json = new SelfDescribingJson(testSchema, testMap);

        // {"schema":"org.test.scheme","data":{}}
        String s = json.toString();

        JSONObject map = new JSONObject(s);
        assertEquals(testSchema, map.getString("schema"));
        JSONObject d = map.getJSONObject("data");
        assertEquals(0, d.length());
    }

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

    public void testCreateWithEmtpyList() throws JSONException {
        SelfDescribingJson json = new SelfDescribingJson(testSchema, testList);

        // {"schema":"org.test.scheme","data":[]}
        String s = json.toString();

        JSONObject map = new JSONObject(s);
        assertEquals(testSchema, map.getString("schema"));
        JSONArray d = map.getJSONArray("data");
        assertEquals(0, d.length());
    }

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

    public void testCreateWithNestedList() throws JSONException {
        List<String> testInnerList = new ArrayList<String>();
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

    public void testCreateWithListOfMaps() throws JSONException {
        testMap.put("a", "b");
        testList.add(testMap);
        testList.add(testMap);
        SelfDescribingJson json = new SelfDescribingJson(testSchema, testList);

        // {"schema":"org.test.scheme","data":[{"a":"b"},{"a":"b"}]}
        String s = json.toString();
        
        // {"schema":"org.test.scheme","data":["{a=b}","{a=b}"]} on pre KITKAT

        JSONObject map = new JSONObject(s);
        assertEquals(testSchema, map.getString("schema"));
        JSONArray list = map.getJSONArray("data");
        assertEquals(2, list.length());
        JSONObject innerListMap1 = list.getJSONObject(0);
        assertEquals("b", innerListMap1.getString("a"));
        JSONObject innerListMap2 = list.getJSONObject(0);
        assertEquals("b", innerListMap2.getString("a"));
    }

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

    /*
     * NOTE this should create the same JSON as above, but the Jackson based implementation
     * returns {"schema":"org.test.scheme","data":{"map":{"schema":"org.test.scheme","data":{}},"node":{"schema":"org.test.scheme","data":"{}"}}}
     * I think that's a bug
     */
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


}