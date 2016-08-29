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

package com.snowplowanalytics.snowplow.tracker.utils;

import android.test.AndroidTestCase;

import java.util.HashMap;
import java.util.Map;

public class FileStoreTest extends AndroidTestCase {

    private String fileName = "test";

    public void setup() {
        Logger.updateLogLevel(LogLevel.DEBUG);
    }

    public void cleanup() {
        FileStore.deleteFile(fileName, getContext());
    }

    public void testSaveMapToFile() {
        setup();
        Map<String, String> map = new HashMap<>();
        map.put("hello", "world");
        boolean result = FileStore.saveMapToFile(fileName, map, getContext());
        assertTrue(result);
        cleanup();

        result = FileStore.saveMapToFile(null, null, getContext());
        assertFalse(result);
    }

    public void testGetMapFromFile() {
        setup();
        Map<String, String> map = new HashMap<>();
        map.put("hello", "world");
        boolean result = FileStore.saveMapToFile(fileName, map, getContext());
        assertTrue(result);

        Map mapRes = FileStore.getMapFromFile(fileName, getContext());
        assertNotNull(mapRes);
        assertTrue(mapRes.containsKey("hello"));
        assertEquals("world", mapRes.get("hello"));
        cleanup();
    }

    public void testDeleteFile() {
        setup();
        Map<String, String> map = new HashMap<>();
        map.put("hello", "world");
        boolean result = FileStore.saveMapToFile(fileName, map, getContext());
        assertTrue(result);

        boolean delRes = FileStore.deleteFile(fileName, getContext());
        assertTrue(delRes);
    }
}
