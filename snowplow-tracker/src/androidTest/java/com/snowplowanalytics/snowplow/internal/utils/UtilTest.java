package com.snowplowanalytics.snowplow.internal.utils;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UtilTest extends AndroidTestCase {

    public void testGetTimestamp() {
        assertEquals(13, Util.getTimestamp().length());
    }

    public void testGetDateTimeFromTimestamp() {
        long timestamp = 1653923456266L;
        assertEquals("2022-05-30T15:10:56.266Z", Util.getDateTimeFromTimestamp(timestamp));
    }

    public void testDateTimeProducesExpectedNumerals() {
        long timestamp = 1660643130123L;
        Locale defaultLocale = Locale.getDefault();

        // set locale to one where different numerals are used (Egypt - arabic)
        Locale.setDefault(new Locale("ar", "EG"));
        assertEquals("2022-08-16T09:45:30.123Z", Util.getDateTimeFromTimestamp(timestamp));

        // restore original locale
        Locale.setDefault(defaultLocale);
    }

    public void testBase64Encode() {
        assertEquals("Zm9v", Util.base64Encode("foo"));
    }

    public void testGetEventId() {
        String eid = Util.getUUIDString();
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
