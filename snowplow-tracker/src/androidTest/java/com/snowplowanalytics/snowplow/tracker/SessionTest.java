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

package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.utils.FileStore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SessionTest extends AndroidTestCase {

    public void testSessionInit() {
        FileStore.deleteFile(TrackerConstants.SNOWPLOW_SESSION_VARS, getContext());
        Session session = new Session(600, 300, TimeUnit.SECONDS, getContext());

        assertNotNull(session);
        assertEquals(600000, session.getForegroundTimeout());
        assertEquals(300000, session.getBackgroundTimeout());
        assertEquals("SQLITE", session.getSessionStorage());
        assertEquals(null, session.getPreviousSessionId());
        assertNotNull(session.getCurrentSessionId());
        assertNotNull(session.getUserId());
        assertEquals(1, session.getSessionIndex());
        assertEquals(null, session.getFirstId());

        SelfDescribingJson sdj = session.getSessionContext("first-id-1");
        assertEquals("first-id-1", session.getFirstId());
        sdj = session.getSessionContext("second-id-2");
        assertEquals("first-id-1", session.getFirstId());

        assertEquals(TrackerConstants.SESSION_SCHEMA, sdj.getMap().get("schema"));
    }

    public void testSessionInitWithInvalidFile() {
        FileStore.deleteFile(TrackerConstants.SNOWPLOW_SESSION_VARS, getContext());

        Map<String, Object> map = new HashMap<>();
        FileStore.saveMapToFile(TrackerConstants.SNOWPLOW_SESSION_VARS, map, getContext());

        Session session = new Session(600, 300, TimeUnit.SECONDS, getContext());

        assertNotNull(session);
        assertEquals(600000, session.getForegroundTimeout());
        assertEquals(300000, session.getBackgroundTimeout());
        assertEquals("SQLITE", session.getSessionStorage());
        assertEquals(null, session.getPreviousSessionId());
        assertNotNull(session.getCurrentSessionId());
        assertNotNull(session.getUserId());
        assertEquals(1, session.getSessionIndex());
        assertEquals(null, session.getFirstId());
    }

    public void testCheckAndUpdateLogicForExpired() {
        FileStore.deleteFile(TrackerConstants.SNOWPLOW_SESSION_VARS, getContext());
        Session session = new Session(0, 0, TimeUnit.SECONDS, getContext());

        String userId = session.getUserId();
        String currentId = session.getCurrentSessionId();

        assertEquals(1, session.getSessionIndex());

        session.checkAndUpdateSession();

        assertEquals(2, session.getSessionIndex());
        assertEquals(userId, session.getUserId());
        assertEquals(currentId, session.getPreviousSessionId());
    }

    public void testCheckAndUpdateLogicForUnExpired() {
        FileStore.deleteFile(TrackerConstants.SNOWPLOW_SESSION_VARS, getContext());
        Session session = new Session(500, 500, TimeUnit.SECONDS, getContext());

        String userId = session.getUserId();
        String currentId = session.getCurrentSessionId();

        assertEquals(1, session.getSessionIndex());

        session.checkAndUpdateSession();

        assertEquals(1, session.getSessionIndex());
        assertEquals(userId, session.getUserId());
        assertEquals(currentId, session.getCurrentSessionId());
    }

    public void testSetBackgroundState() {
        FileStore.deleteFile(TrackerConstants.SNOWPLOW_SESSION_VARS, getContext());
        Session session = new Session(500, 0, TimeUnit.SECONDS, getContext());

        assertEquals(1, session.getSessionIndex());
        session.checkAndUpdateSession();
        assertEquals(1, session.getSessionIndex());
        session.setIsBackground(true);
        session.checkAndUpdateSession();
        assertEquals(2, session.getSessionIndex());
    }
}
