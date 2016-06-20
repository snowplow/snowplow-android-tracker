package com.snowplowanalytics.snowplow.tracker.utils;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.List;

public class UtilTest extends AndroidTestCase {

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
}
