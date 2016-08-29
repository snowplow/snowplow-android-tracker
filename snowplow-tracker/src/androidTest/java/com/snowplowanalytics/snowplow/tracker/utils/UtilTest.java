package com.snowplowanalytics.snowplow.tracker.utils;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.storage.EventStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UtilTest extends AndroidTestCase {

    public void testGetTimestamp() {
        assertEquals(13, Util.getTimestamp().length());
    }

    public void testBase64Encode() {
        assertEquals("Zm9v", Util.base64Encode("foo"));
    }

    public void testGetEventId() {
        String eid = Util.getEventId();
        assertNotNull(eid);
        assertTrue(eid.matches("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$"));
    }

    public void testGetUTF8Length() {
        assertEquals(19, Util.getUTF8Length("foo€♥£\uD800\uDF48\uD83C\uDF44"));
    }

    public void testJoinLongList() {
        List<Long> list = new ArrayList<>();
        list.add((long)1);
        assertEquals("1", Util.joinLongList(list));
        list.add((long)2);
        list.add((long)3);
        assertEquals("1,2,3", Util.joinLongList(list));
        list.add(null);
        assertEquals("1,2,3", Util.joinLongList(list));
        list.add((long)5);
        assertEquals("1,2,3,5", Util.joinLongList(list));
    }

    public void testMobileContext() {
        SelfDescribingJson sdj = Util.getMobileContext(getContext());
        assertNotNull(sdj);

        Map<String, Object> sdjMap = sdj.getMap();
        assertEquals((String) sdjMap.get("schema"), TrackerConstants.MOBILE_SCHEMA);

        Map sdjData = (Map) sdjMap.get("data");
        assertEquals((String) sdjData.get(Parameters.OS_TYPE), "android");
        assertTrue(sdjData.containsKey(Parameters.OS_VERSION));
        assertTrue(sdjData.containsKey(Parameters.DEVICE_MODEL));
        assertTrue(sdjData.containsKey(Parameters.DEVICE_MANUFACTURER));
        assertTrue(sdjData.containsKey(Parameters.CARRIER));
        assertTrue(sdjData.containsKey(Parameters.NETWORK_TECHNOLOGY));
        assertTrue(sdjData.containsKey(Parameters.NETWORK_TYPE));
    }

    public void testDeserialize() {
        Map<String, String> testMap = new HashMap<>();
        testMap.put("foo", "bar");

        byte[] testMapBytes = Util.serialize(testMap);
        assertNotNull(testMapBytes);

        Map<String, String> testMap2 = Util.deserializer(testMapBytes);
        assertNotNull(testMap2);
        assertEquals("bar", testMap2.get("foo"));

        Map<String, String> d = Util.deserializer(null);
        assertNull(d);
    }

    public void testMapHasKeys() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");

        assertTrue(Util.mapHasKeys(map, "key"));
        assertFalse(Util.mapHasKeys(map, "key2"));
        assertFalse(Util.mapHasKeys(map, "key", "key2"));
    }

    public void testAddToMap() {
        Map<String, Object> map = new HashMap<>();
        Util.addToMap(null, null, map);
        assertEquals(0, map.size());
        Util.addToMap("hello", null, map);
        assertEquals(0, map.size());
        Util.addToMap("", "", map);
        assertEquals(0, map.size());
        Util.addToMap("hello", "world", map);
        assertEquals(1, map.size());
        assertEquals("world", map.get("hello"));
    }
}
