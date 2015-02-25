package com.snowplowanalytics.snowplow.tracker.utils.payload;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class SelfDescribingJsonTest extends AndroidTestCase {

    private final String testSchema = "org.test.scheme";
    private HashMap<String, Object> testMap;
    private List<Object> testList;

    public void setUp() {
        testMap = new HashMap<String,Object>();
        testList = new ArrayList<Object>();
    }

    public void testCreateWithSchemaOnly() {
        SelfDescribingJson json = new SelfDescribingJson(testSchema);
        assertEquals("{\"schema\":\"org.test.scheme\",\"data\":{}}", json.toString());
    }

    public void testCreateWithNullSchema() {
        // TODO fill in
    }

    public void testCreateWithEmptySchema() {
        // TODO fill in
    }

    public void testCreateWithOurEmptyMap() {
        SelfDescribingJson json = new SelfDescribingJson(testSchema, testMap);
        assertEquals("{\"schema\":\"org.test.scheme\",\"data\":{}}", json.toString());
    }

    public void testCreateWithSimpleMap() {
        testMap.put("alpha", "beta");
        SelfDescribingJson json = new SelfDescribingJson(testSchema, testMap);
        assertEquals("{\"schema\":\"org.test.scheme\",\"data\":{\"alpha\":\"beta\"}}", json.toString());
    }

    public void testCreateWithEmtpyList() {
        SelfDescribingJson json = new SelfDescribingJson(testSchema, testList);
        assertEquals("{\"schema\":\"org.test.scheme\",\"data\":[]}", json.toString());
    }

    public void testCreateWithSimpleList() {
        testList.add("delta");
        SelfDescribingJson json = new SelfDescribingJson(testSchema, testList);
        assertEquals("{\"schema\":\"org.test.scheme\",\"data\":[\"delta\"]}", json.toString());
    }

    public void testCreateWithNestedList() {
        List<String> innerList = new ArrayList<String>();
        innerList.add("gamma");
        innerList.add("epsilon");
        testList.add(innerList);
        SelfDescribingJson json = new SelfDescribingJson(testSchema, testList);
        assertEquals("{\"schema\":\"org.test.scheme\",\"data\":[[\"gamma\",\"epsilon\"]]}", json.toString());
    }

    public void testCreateWithListOfMaps() {
        testMap.put("a", "b");
        testList.add(testMap);
        testList.add(testMap);
        SelfDescribingJson json = new SelfDescribingJson(testSchema, testList);
        assertEquals("{\"schema\":\"org.test.scheme\",\"data\":[{\"a\":\"b\"},{\"a\":\"b\"}]}", json.toString());
    }

    public void testCreateWithSelfDescribingJson() {
        SelfDescribingJson json = new SelfDescribingJson(testSchema, new SelfDescribingJson(testSchema, testMap));
        assertEquals("{\"schema\":\"org.test.scheme\",\"data\":{\"schema\":\"org.test.scheme\",\"data\":{}}}", json.toString());
    }

    public void testCreateWithTrackerPayload() {
        TrackerPayload payload = new TrackerPayload();
        testMap.put("a", "b");
        payload.addMap(testMap);
        SelfDescribingJson json = new SelfDescribingJson(testSchema, payload);
        assertEquals("{\"schema\":\"org.test.scheme\",\"data\":{\"a\":\"b\"}}", json.toString());
    }



}