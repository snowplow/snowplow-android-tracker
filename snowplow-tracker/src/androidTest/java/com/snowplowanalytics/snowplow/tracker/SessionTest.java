/*
 * Copyright (c) 2015-2019 Snowplow Analytics Ltd. All rights reserved.
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

import android.content.Context;
import android.content.SharedPreferences;
import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SessionTest extends AndroidTestCase {

    public void testSessionInit() {
        Session session = getSession(600, 300);

        assertNotNull(session);
        assertEquals(600000, session.getForegroundTimeout());
        assertEquals(300000, session.getBackgroundTimeout());
        assertEquals("SQLITE", session.getSessionStorage());
        assertNull(session.getPreviousSessionId());
        assertNotNull(session.getCurrentSessionId());
        assertNotNull(session.getUserId());
        assertEquals(1, session.getSessionIndex());
        assertNull(session.getFirstId());

        SelfDescribingJson sdj = session.getSessionContext("first-id-1");
        assertEquals("first-id-1", session.getFirstId());
        sdj = session.getSessionContext("second-id-2");
        assertEquals("first-id-1", session.getFirstId());

        assertEquals(TrackerConstants.SESSION_SCHEMA, sdj.getMap().get("schema"));
    }


    public void testCheckAndUpdateLogicForExpired() {
        Session session = getSession(0, 0);

        String userId = session.getUserId();
        String currentId = session.getCurrentSessionId();

        assertEquals(1, session.getSessionIndex());

        session.checkAndUpdateSession();

        assertEquals(2, session.getSessionIndex());
        assertEquals(userId, session.getUserId());
        assertEquals(currentId, session.getPreviousSessionId());
    }

    public void testCheckAndUpdateLogicForUnExpired() {
        Session session = getSession(500, 500);

        String userId = session.getUserId();
        String currentId = session.getCurrentSessionId();

        assertEquals(1, session.getSessionIndex());

        session.checkAndUpdateSession();

        assertEquals(1, session.getSessionIndex());
        assertEquals(userId, session.getUserId());
        assertEquals(currentId, session.getCurrentSessionId());
    }

    public void testSetBackgroundState() {
        Session session = getSession(500, 0);

        assertEquals(1, session.getSessionIndex());
        session.checkAndUpdateSession();
        assertEquals(1, session.getSessionIndex());
        session.setIsBackground(true);
        session.checkAndUpdateSession();
        assertEquals(2, session.getSessionIndex());
    }

    public void testSessionInitWithIncompletePersistedData() {
        SharedPreferences prefs = getContext().getSharedPreferences(TrackerConstants.SNOWPLOW_SESSION_VARS, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        prefs.edit().putString(Parameters.SESSION_USER_ID, UUID.randomUUID().toString()).apply();

        Session session = new Session(600, 300, TimeUnit.SECONDS, getContext());
        session.waitForSessionFileLoad();

        assertNotNull(session);
        assertEquals(600000, session.getForegroundTimeout());
        assertEquals(300000, session.getBackgroundTimeout());
        assertEquals("SQLITE", session.getSessionStorage());
        assertNull(session.getPreviousSessionId());
        assertNotNull(session.getCurrentSessionId());
        assertNotNull(session.getUserId());
        assertEquals(1, session.getSessionIndex());
        assertNull(session.getFirstId());
    }


    private Session getSession(long foregroundTimeout, long backgroundTimeout) {
        getContext().getSharedPreferences(TrackerConstants.SNOWPLOW_SESSION_VARS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
        Session session = new Session(foregroundTimeout, backgroundTimeout, TimeUnit.SECONDS, getContext());
        session.waitForSessionFileLoad();
        return session;
    }
}
